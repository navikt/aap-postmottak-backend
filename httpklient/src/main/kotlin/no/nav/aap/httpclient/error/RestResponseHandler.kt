package no.nav.aap.httpclient.error

import java.net.http.HttpRequest
import java.net.http.HttpResponse

interface RestResponseHandler {

    fun <R> håndter(request: HttpRequest, response: HttpResponse<String>, mapper: (String) -> R) : R?
}