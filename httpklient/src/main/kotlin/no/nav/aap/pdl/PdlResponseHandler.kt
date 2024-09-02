package no.nav.aap.pdl

import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.error.RestResponseHandler
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class PdlResponseHandler() : RestResponseHandler<String> {

    private val defaultErrorHandler = DefaultResponseHandler()

    override fun <R> håndter(request: HttpRequest, response: HttpResponse<String>, mapper: (String, HttpHeaders) -> R): R? {
        val respons = defaultErrorHandler.håndter(request, response, mapper)

        if (respons != null && respons is PdlResponse) {
            if (respons.errors?.isNotEmpty() == true) {
                throw PdlQueryException(
                    String.format(
                        "Feil %s ved GraphQL oppslag mot %s",
                        respons.errors.map(GraphQLError::message).joinToString(), request.uri()
                    )
                )
            }
        }

        return respons
    }

    override fun bodyHandler(): HttpResponse.BodyHandler<String> {
        return HttpResponse.BodyHandlers.ofString()
    }
}

class PdlQueryException(msg: String) : RuntimeException(msg)