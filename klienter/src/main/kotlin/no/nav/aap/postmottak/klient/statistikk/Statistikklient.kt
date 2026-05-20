package no.nav.aap.postmottak.klient.statistikk

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.StatistikkGateway
import no.nav.aap.postmottak.kontrakt.hendelse.DokumentflytStoppetHendelse
import java.net.URI

class StatistikkKlient : StatistikkGateway {
    private val baseUrl = URI.create(requiredConfigForKey("INTEGRASJON_STATISTIKK_URL"))
    private val config = ClientConfig(scope = requiredConfigForKey("INTEGRASJON_STATISTIKK_SCOPE"))


    companion object : Factory<StatistikkKlient> {
        private val statistikkKlient by lazy { StatistikkKlient() }
        override fun konstruer(): StatistikkKlient = statistikkKlient
    }

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = AzureM2MTokenProvider,
        prometheus = PrometheusProvider.prometheus
    )

    override fun avgiHendelse(oppgaveHendelse: DokumentflytStoppetHendelse) {
        val httpRequest = PostRequest(
            body = oppgaveHendelse,
        )
        requireNotNull(
            client.post<_, Unit>(
                uri = baseUrl.resolve("/postmottak"),
                request = httpRequest
            )
        )
    }
}