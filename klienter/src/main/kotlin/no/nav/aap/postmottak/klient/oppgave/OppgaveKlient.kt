package no.nav.aap.postmottak.klient.oppgave

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.lookup.gateway.Factory
import no.nav.aap.postmottak.gateway.OppgaveGateway
import no.nav.aap.postmottak.kontrakt.hendelse.DokumentflytStoppetHendelse
import java.net.URI

class OppgaveKlient: OppgaveGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.oppgave.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.oppgave.scope"))

    companion object: Factory<OppgaveKlient> {
        override fun konstruer(): OppgaveKlient {
            return OppgaveKlient()
        }
    }
    
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    override fun varsleHendelse(hendelse: DokumentflytStoppetHendelse) {
        client.post<_, Unit>(url.resolve("/oppdater-postmottak-oppgaver"), PostRequest(body = hendelse))
    }
}
