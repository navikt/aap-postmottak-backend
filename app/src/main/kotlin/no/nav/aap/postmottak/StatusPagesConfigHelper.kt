package no.nav.aap.postmottak

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.response.respond
import no.nav.aap.komponenter.httpklient.exception.ApiErrorCode
import no.nav.aap.komponenter.httpklient.exception.ApiException
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import no.nav.aap.komponenter.httpklient.exception.TimeoutException
import no.nav.aap.komponenter.httpklient.httpclient.error.ManglerTilgangException
import no.nav.aap.postmottak.avklaringsbehov.AvslagException
import no.nav.aap.postmottak.avklaringsbehov.BehandlingUnderProsesseringException
import no.nav.aap.postmottak.avklaringsbehov.OutdatedBehandlingException
import no.nav.aap.postmottak.exception.FlytOperasjonException
import org.slf4j.LoggerFactory
import java.net.http.HttpTimeoutException

object StatusPagesConfigHelper {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun setup(): StatusPagesConfig.() -> Unit = {
        exception<Throwable> { call, cause ->
            val logger = LoggerFactory.getLogger(javaClass)

            when (cause) {
                is InternfeilException -> {
                    logger.error(cause.cause?.message ?: cause.message)
                    call.respondWithError(cause)
                }

                is ApiException -> {
                    logger.warn(cause.message, cause)
                    call.respondWithError(cause)
                }

                is HttpRequestTimeoutException,
                is HttpTimeoutException -> {
                    logger.warn("Timeout", cause)
                    call.respondWithError(
                        ApiException(
                            status = HttpStatusCode.RequestTimeout,
                            message = "Forespørselen tok for lang tid. Prøv igjen om litt."
                        )
                    )
                }

                is ManglerTilgangException -> {
                    val uri = call.request.local.uri
                    logger.info("Ikke tilgang til endepunkt $uri.", cause)
                    call.respondWithError(
                        IkkeTillattException(
                            message = "Ikke tilgang til ekstern integrasjon."
                        )
                    )
                }

                is FlytOperasjonException -> {
                    call.respondWithError(
                        ApiException(
                            status = when (cause) {
                                is AvslagException -> HttpStatusCode.NotImplemented
                                is BehandlingUnderProsesseringException -> HttpStatusCode.Conflict
                                is OutdatedBehandlingException -> HttpStatusCode.Conflict
                                else -> HttpStatusCode.InternalServerError
                            },
                            message = cause.body().message ?: "Ukjent feil i behandlingsflyt"
                        )
                    )
                }

                is ClientRequestException -> {
                    val uri = call.request.local.uri

                    if (cause.response.status == HttpStatusCode.RequestTimeout) {
                        logger.warn("Timeout ved kall til '$uri'", cause)
                        call.respondWithError(TimeoutException("Forespørselen tok for lang tid. Prøv igjen om litt."))
                    } else {
                        logger.error("Feil ved kall til '$uri'.", cause)
                        call.respondWithError(
                            ApiException(
                                status = cause.response.status,
                                message = "Feil ved kall til '$uri'."
                            )
                        )
                    }
                }

                else -> {
                    logger.error("Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                    call.respondWithError(InternfeilException("En ukjent feil oppsto"))
                }
            }
        }

        status(HttpStatusCode.NotFound) { call, _ ->
            val req = call.request.local
            logger.error("Fikk kall mot endepunkt som ikke finnes (${req.method}: ${req.uri})")

            call.respondWithError(
                ApiException(
                    status = HttpStatusCode.NotFound,
                    message = "Kunne ikke nå endepunkt: ${req.uri}",
                    code = ApiErrorCode.ENDEPUNKT_IKKE_FUNNET
                )
            )
        }
    }

    private suspend fun ApplicationCall.respondWithError(exception: ApiException) {
        respond(
            exception.status,
            exception.tilApiErrorResponse()
        )
    }
}