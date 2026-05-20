package no.nav.aap.postmottak.klient.nom

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.EgenAnsattGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import java.net.URI

data class EgenansattRequest(val personident: String)

class NomKlient : EgenAnsattGateway {

    private val url = URI.create(requiredConfigForKey("INTEGRASJON_NOM_URL"))

    private val config = ClientConfig(
        scope = requiredConfigForKey("INTEGRASJON_NOM_SCOPE"),
    )

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = AzureM2MTokenProvider,
        prometheus = PrometheusProvider.prometheus
    )

    companion object : Factory<NomKlient> {
        private val nomKlient by lazy { NomKlient() }
        override fun konstruer(): NomKlient = nomKlient
    }

    override fun erEgenAnsatt(ident: Ident): Boolean {
        val egenansattUrl = url.resolve("skjermet")
        val request = PostRequest(
            body = EgenansattRequest(ident.identifikator)
        )

        return client.post(egenansattUrl, request) ?: error("Uventet respons fra NOM")
    }

}
