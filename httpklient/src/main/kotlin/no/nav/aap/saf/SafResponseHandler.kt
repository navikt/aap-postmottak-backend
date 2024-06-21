package no.nav.aap.saf

import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.error.DefaultResponseHandler
import no.nav.aap.httpclient.error.RestResponseHandler
import no.nav.aap.pdl.GraphQLError
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class SafResponseHandler(config: ClientConfig) : RestResponseHandler {

    private val defaultErrorHandler = DefaultResponseHandler(config)

    override fun <R> håndter(
        request: HttpRequest,
        response: HttpResponse<String>,
        mapper: (String, HttpHeaders) -> R
    ): R? {
        val respons = defaultErrorHandler.håndter(request, response, mapper)

        if (respons != null && respons is SafResponse) {
            if (respons.errors?.isNotEmpty() == true) {
                throw SafQueryException(
                    String.format(
                        "Feil %s ved GraphQL oppslag mot %s",
                        respons.errors.map(GraphQLError::message).joinToString(), request.uri()
                    )
                )
            }
        }

        return respons
    }
}

class SafQueryException(msg: String) : RuntimeException(msg)