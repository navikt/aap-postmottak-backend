package no.nav.aap.postmottak.klient.arena

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.net.URI

class ArenaKlient() {

    private val url = URI.create(requiredConfigForKey("integrasjon.arena.url.rest"))

    private var client = RestClient.withDefaultResponseHandler(
        config = ClientConfig(requiredConfigForKey("integrasjon.arena.url")),
        tokenProvider = ClientCredentialsTokenProvider
    )

    fun nyesteArenaSak(ident: Ident): String? {
        val request = GetRequest()
        val nyesteSakUrl = url.resolve("arena/nyesteaktivesak/${ident.identifikator}")
        return client.get(nyesteSakUrl, request)
    }

    fun opprettArenaOppgave(arenaOpprettetForespørsel: ArenaOpprettOppgaveForespørsel): ArenaOpprettOppgaveRespons {
        val request = PostRequest(arenaOpprettetForespørsel)
        val opprettArenaoppgaveUrl = url.resolve("arena/opprettoppgave")
        return client.post(opprettArenaoppgaveUrl, request) ?: error("Ingen respons fra Arena")
    }

}
