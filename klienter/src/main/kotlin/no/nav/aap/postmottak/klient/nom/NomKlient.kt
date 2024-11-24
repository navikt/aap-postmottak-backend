package no.nav.aap.postmottak.klient.nom

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.net.URI

data class EgenansattRequest(val personident: String)

class NomKlient {

    private val url = URI.create(requiredConfigForKey("integrasjon.nom.url"))

    private val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.nom.scope"),
    )

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider
    )

    fun erEgenansatt(ident: Ident): Boolean {
        val egenansattUrl = url.resolve("egenansatt")
        val request = PostRequest(
            body = EgenansattRequest(ident.identifikator)
        )

        return client.post(egenansattUrl, request) ?: error("Uventet respons fra NOM")
    }

}
