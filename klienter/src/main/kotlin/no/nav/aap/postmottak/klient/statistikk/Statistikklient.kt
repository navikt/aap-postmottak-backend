package no.nav.aap.postmottak.klient.statistikk

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.StatistikkGateway
import no.nav.aap.postmottak.kontrakt.hendelse.DokumentflytStoppetHendelse
import java.net.URI

class StatistikkKlient : StatistikkGateway {
    private val baseUrl = URI.create(requiredConfigForKey("integrasjon.statistikk.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.statistikk.scope"))


    companion object : Factory<StatistikkKlient> {
        override fun konstruer(): StatistikkKlient {
            return StatistikkKlient()
        }
    }

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
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