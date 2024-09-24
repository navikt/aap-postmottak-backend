package no.nav.aap.postmottak.hendelse.oppgave

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI

object OppgaveGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.oppgave.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.oppgave.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    fun varsleHendelse(hendelse: BehandlingsFlytStoppetHendelseDTO) {
        // TODO: Bruk riktig endepunkt
        client.post<_, Unit>(url.resolve("/behandling"), PostRequest(body = hendelse))
    }
}
