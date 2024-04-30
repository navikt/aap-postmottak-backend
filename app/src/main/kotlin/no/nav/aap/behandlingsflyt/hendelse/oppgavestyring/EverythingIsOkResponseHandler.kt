package no.nav.aap.behandlingsflyt.hendelse.oppgavestyring

import no.nav.aap.httpclient.error.RestResponseHandler
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object EverythingIsOkResponseHandler : RestResponseHandler {
    override fun <R> håndter(request: HttpRequest, response: HttpResponse<String>, mapper: (String) -> R): R? {
        return null
    }
}
