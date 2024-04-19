package no.nav.aap.httpclient.error

import no.nav.aap.httpclient.ClientConfig
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.http.HttpRequest
import java.net.http.HttpResponse


class DefaultResponseHandler(private val config: ClientConfig) : RestResponseHandler {

    private val SECURE_LOGGER = LoggerFactory.getLogger("secureLog")

    override fun <R> håndter(request: HttpRequest, response: HttpResponse<String>, mapper: (String) -> R): R? {
        val status: Int = response.statusCode()
        if (status == HttpURLConnection.HTTP_NO_CONTENT) {
            return null
        }
        if ((status >= HttpURLConnection.HTTP_OK && status < HttpURLConnection.HTTP_MULT_CHOICE)
            || config.isParseableStatus(status)
        ) {
            val value = response.body()
            SECURE_LOGGER.info(value)
            return mapper(value)
        }
        if (status == HttpURLConnection.HTTP_BAD_REQUEST) {
            SECURE_LOGGER.info(response.body())
            throw UhåndtertHttpResponsException("Bad request mot ${request.uri()}")
        }
        if (status == HttpURLConnection.HTTP_FORBIDDEN) {
            throw ManglerTilgangException("Feilet mot ${request.uri()}")
        }

        throw UhåndtertHttpResponsException("Uventet httprespons kode $status")
    }
}