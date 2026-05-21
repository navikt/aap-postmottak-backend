package no.nav.aap.postmottak.klient.arena

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
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


private val secureLog = LoggerFactory.getLogger("team-logs")
private val log = LoggerFactory.getLogger(ArenaoppslagGatewayImpl::class.java)

private val objectMapper = jacksonObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .registerModule(JavaTimeModule())

private fun responseStatus(throwable: Throwable): HttpStatusCode? =
    generateSequence(throwable) { it.cause }
        .filterIsInstance<ResponseException>()
        .firstOrNull()?.response?.status

private val defaultHttpClient = HttpClient(CIO) {
    expectSuccess = true // Kaster exception for 4xx og 5xx svar

    install(HttpTimeout) {
        requestTimeoutMillis = 20.seconds.inWholeMilliseconds // normalt under 1s
        connectTimeoutMillis = 20.seconds.inWholeMilliseconds
        socketTimeoutMillis = 20.seconds.inWholeMilliseconds
    }

    install(HttpRequestRetry) {
        // Retry på transiente nettverksfeil og 5xx – ikke på 4xx-klientfeil og ikke på timeouts.
        // Timeouts retries ikke fordi vi heller vil feile raskt enn å akkumulere ventetid.
        retryOnExceptionIf(maxRetries = 3) { _, cause ->
            responseStatus(cause) != HttpStatusCode.NotFound &&
                    cause !is HttpRequestTimeoutException &&
                    cause !is ConnectTimeoutException &&
                    cause !is SocketTimeoutException
        }
        retryOnServerErrors(maxRetries = 3) // 5xx
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
    ).recover { throwable ->
        if (responseStatus(throwable) == HttpStatusCode.NotFound) {
            secureLog.error("Personen ble ikke funnet i Arena [personidentifikator=${req.personidentifikator}]")
            // Personen ble ikke funnet i Arena – returner tom liste med saker
            MaksdatoResponse(emptyList())
        } else {
            throw throwable
        }
    }.getOrThrow()

    suspend fun hentSisteUtbetalingsDatoISaker(
        req: SisteUtbetalingerRequest
    ): SisteUtbetalingerResponse = gjørArenaOppslag<SisteUtbetalingerResponse, SisteUtbetalingerRequest>(
        "/api/v1/utbetalinger/siste", req
    ).recover { throwable ->
        if (responseStatus(throwable) == HttpStatusCode.NotFound) {
            secureLog.error("Personen ble ikke funnet i Arena [personidentifikator=${req.personidentifikator}]")
            // Personen ble ikke funnet i Arena – returner tom dato
            SisteUtbetalingerResponse(null)
        } else {
            throw throwable
        }
    }.getOrThrow()

    private suspend inline fun <reified T, reified V> gjørArenaOppslag(
        endepunkt: String, req: V
    ): Result<T> = runCatching {
        val url = URLBuilder(config.proxyBaseUrl)
            .appendPathSegments(endepunkt)
            .buildString()

        val token = try {
            tokenProvider.getClientCredentialToken(config.scope)
        } catch (e: Exception) {
            throw RuntimeException("Fetch av token for Arena-oppslag feilet", e)
        }

        val response = try {
            arenaHttpClient.post(url) {
                accept(ContentType.Application.Json)
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        } catch (e: Exception) {
            throw RuntimeException("Fetch av Arena-data feilet for '$endepunkt'", e)
        }

        try {
            objectMapper.readValue<T>(response.bodyAsText())
        } catch (e: Exception) {
            throw RuntimeException("Parsefeil for '$endepunkt'", e)
        }
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
