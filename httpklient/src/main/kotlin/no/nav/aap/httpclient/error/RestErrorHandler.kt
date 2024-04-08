package no.nav.aap.httpclient.error

import java.net.http.HttpRequest
import java.net.http.HttpResponse

interface RestErrorHandler {

    fun <R> håndter(request: HttpRequest, response: HttpResponse<String>, clazz: Class<R>) : R?
}