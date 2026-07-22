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
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.*
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoMedVedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteUtbetalingerRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteUtbetalingerResponse
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.gateway.ArenasakForManuellVurdering
import no.nav.aap.postmottak.gateway.SisteAapVedtak
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

    suspend fun harHistorikk(
        req: HarHistorikkRequest
    ): HarHistorikkResponse =
        gjørArenaOppslag<HarHistorikkResponse>(
            "/api/v1/person/historikk", req
        ).getOrThrow()

    suspend fun harSignifikantHistorikk(
        req: SignifikantHistorikkRequest
    ): SignifikantHistorikkResponse =
        gjørArenaOppslag<SignifikantHistorikkResponse>(
            "/api/v1/person/historikk/signifikant", req
        ).getOrThrow()

    suspend fun hentMaksdatoISisteVedtak(
        req: MaksdatoRequest
    ): MaksdatoMedVedtakResponse = gjørArenaOppslag<MaksdatoMedVedtakResponse>(
        "/api/v1/person/maksdato", req
    ).recover { throwable ->
        if (responseStatus(throwable) == HttpStatusCode.NotFound) {
            secureLog.error("Personen ble ikke funnet i Arena [personidentifikator=${req.personidentifikator}]")
            // Vedtak for personen ble ikke funnet i Arena – returner ingen sak
            MaksdatoMedVedtakResponse(null)
        } else {
            throw throwable
        }
    }.getOrThrow()

    suspend fun hentSisteUtbetalingsDatoISaker(
        req: SisteUtbetalingerRequest
    ): SisteUtbetalingerResponse = gjørArenaOppslag<SisteUtbetalingerResponse>(
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

    @Suppress("TooGenericExceptionThrown", "TooGenericExceptionCaught")
    private suspend inline fun <reified T> gjørArenaOppslag(
        endepunkt: String,
        body: Any? = null,
        method: HttpMethod = HttpMethod.Post,
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
            arenaHttpClient.request(url) {
                this.method = method
                accept(ContentType.Application.Json)
                bearerAuth(token)
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
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

    override suspend fun harHistorikk(person: Person): Boolean {
        val request = HarHistorikkRequest(person.aktivIdent().identifikator)
        val response = harHistorikk(request)
        return response.harHistorikk
    }

    override suspend fun harSignifikantHistorikk(
        person: Person, mottattDato: LocalDate
    ): SignifikantHistorikkResponse {
        val request = SignifikantHistorikkRequest(person.aktivIdent().identifikator, mottattDato)
        val response = harSignifikantHistorikk(request)
        return response
    }

    override suspend fun sisteVedtakMedMaksdato(ident: Ident): SakMedSisteVedtakOgMaksdato? {
        val request = MaksdatoRequest(ident.identifikator)
        val response = hentMaksdatoISisteVedtak(request)
        return response.sak
    }

    override suspend fun sisteUtbetalingsdatoForPerson(ident: Ident): LocalDate? {
        val request = SisteUtbetalingerRequest(ident.identifikator)
        val response = hentSisteUtbetalingsDatoISaker(request)
        return response.utbetalingsdato
    }

    override suspend fun hentArenasakForManuellVurdering(ident: Ident): ArenasakForManuellVurdering {
        // TODO: Arena-API-et er ikke implementert enda – returnerer dummy-data inntil videre.
        log.info("hentArenasakForManuellVurdering er ikke implementert mot Arena enda – returnerer dummy-data")
        return ArenasakForManuellVurdering(
            saksnummer = "2024-23456",
            aktiv = false,
            under52 = true,
            gjenstaendeOrdinaerPeriodeDager = 67,
            gjenstaendeUnntaksperiodeDager = null,
            sisteAapVedtak = SisteAapVedtak(
                paragraf = "§ 11-18",
                beskrivelse = "Under vurdering for uføretrygd",
                fom = null,
                tom = null,
            ),
            sisteUtbetaling = null,
            navKontoretsInnstillingUrl = null,
        )
    }

}
