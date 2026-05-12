package no.nav.aap.postmottak.klient.arena

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteUtbetalingerRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteUtbetalingerResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerResponse
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds


private val log = LoggerFactory.getLogger(ArenaoppslagGatewayImpl::class.java)

private val objectMapper = jacksonObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .registerModule(JavaTimeModule())

private val defaultHttpClient = HttpClient(CIO) {
    expectSuccess = true // Kaster exception for 4xx og 5xx svar

    install(HttpTimeout) {
        requestTimeoutMillis = 20.seconds.inWholeMilliseconds
        connectTimeoutMillis = 20.seconds.inWholeMilliseconds
        socketTimeoutMillis = 20.seconds.inWholeMilliseconds
    }

    install(HttpRequestRetry) {
        retryOnException(maxRetries = 3) // retry on exception during network send, other than timeout exceptions
        exponentialDelay()
    }

    install(ContentNegotiation) {
        jackson {
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            registerModule(JavaTimeModule())
        }
    }
}


class ArenaoppslagGatewayImpl : ArenaoppslagGateway {
    val arenaHttpClient = defaultHttpClient
    val tokenProvider = AzureAdTokenProvider(defaultHttpClient)
    val config = ArenaoppslagConfig()

    companion object : Factory<ArenaoppslagGatewayImpl> {
        override fun konstruer(): ArenaoppslagGatewayImpl {
            return ArenaoppslagGatewayImpl()
        }
    }

    suspend fun hentPersonEksistererIAapContext(
        req: SakerRequest,
    ): PersonEksistererIAAPArena = gjørArenaOppslag<PersonEksistererIAAPArena, SakerRequest>(
        "/api/v1/person/eksisterer", req
    ).getOrThrow()

    suspend fun personHarSignifikantAAPArenaHistorikk(
        req: SignifikanteSakerRequest
    ): SignifikanteSakerResponse = gjørArenaOppslag<SignifikanteSakerResponse, SignifikanteSakerRequest>(
        "/api/v1/person/signifikant-historikk", req
    ).getOrThrow()

    suspend fun hentMaksdatoISaker(
        req: MaksdatoRequest
    ): MaksdatoResponse = gjørArenaOppslag<MaksdatoResponse, MaksdatoRequest>(
        "/api/v1/maksdato", req
    ).getOrThrow()

    suspend fun hentSisteUtbetalingsDatoISaker(
        req: SisteUtbetalingerRequest
    ): SisteUtbetalingerResponse = gjørArenaOppslag<SisteUtbetalingerResponse, SisteUtbetalingerRequest>(
        "/api/v1/utbetalinger/siste", req
    ).getOrThrow()


    private suspend inline fun <reified T, reified V> gjørArenaOppslag(
        endepunkt: String, req: V
    ): Result<T> {
        // Vi starter en kjede av kall og prosessering, hvor hvert steg kan feile.
        var fikkToken = false
        var fikkArenaData = false

        val parsedResult = runCatching {
            val token = tokenProvider.getClientCredentialToken(config.scope).also {
                fikkToken = true
            }

            val arenaOppslagEndepunkt = URLBuilder(config.proxyBaseUrl)
                .appendPathSegments(endepunkt)
                .buildString()
            val arenaResponse = arenaHttpClient.post(arenaOppslagEndepunkt) {
                accept(ContentType.Application.Json)
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(req)
            }.also {
                if (it.status.isSuccess()) {
                    fikkArenaData = true
                }
            }

            objectMapper.readValue<T>(arenaResponse.bodyAsText())
        }.onFailure { e ->
            when {
                !fikkToken -> log.error("Fetch av token for Arena-oppslag feilet", e)
                !fikkArenaData -> log.error("Fetch av Arena-data feilet for '$endepunkt'", e)
                else -> {
                    log.error("Parsefeil for '$endepunkt'", e)
                }
            }
        }
        return parsedResult
    }

    override suspend fun harAapSakIArena(person: Person): Boolean {
        val request = SakerRequest(person.identer().map { it.identifikator })
        val response = hentPersonEksistererIAapContext(request)
        return response.eksisterer
    }

    override suspend fun harSignifikantHistorikkIAAPArena(person: Person, mottattDato: LocalDate) =
        hentSakerMedSignifikantHistorikk(person, mottattDato).isNotEmpty()

    override suspend fun hentSakerMedSignifikantHistorikk(
        person: Person, mottattDato: LocalDate
    ): List<Int> {
        val request = SignifikanteSakerRequest(person.identer().map { it.identifikator }, mottattDato)
        val response = personHarSignifikantAAPArenaHistorikk(request)
        return response.signifikanteSaker.map { it.toInt() }
    }

    override suspend fun maksdatoForSaker(ident: Ident): List<SakMedSisteVedtakOgMaksdato> {
        val request = MaksdatoRequest(ident.identifikator)
        val response = hentMaksdatoISaker(request)
        return response.sakliste
    }

    override suspend fun sisteUtbetalingsdatoForPerson(ident: Ident): LocalDate? {
        val request = SisteUtbetalingerRequest(ident.identifikator)
        val response = hentSisteUtbetalingsDatoISaker(request)
        return response.utbetalingsdato
    }

}
