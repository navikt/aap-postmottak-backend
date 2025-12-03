package no.nav.aap.postmottak.klient.arena

import no.nav.aap.fordeler.arena.ArenaGateway
import no.nav.aap.fordeler.arena.ArenaOpprettOppgaveForespørsel
import no.nav.aap.fordeler.arena.ArenaOpprettOppgaveRespons
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.net.URI

class ArenaKlient : ArenaGateway {

    private val url = URI.create(requiredConfigForKey("integrasjon.aap.fss.proxy.url"))

    private var client = RestClient.withDefaultResponseHandler(
        config = ClientConfig(requiredConfigForKey("integrasjon.aap.fss.proxy.scope")),
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = PrometheusProvider.prometheus
    )

    companion object : Factory<ArenaKlient> {
        override fun konstruer(): ArenaKlient {
            return ArenaKlient()
        }
    }

    override fun harAktivSak(ident: Ident) = nyesteAktiveSak(ident) != null

    override fun nyesteAktiveSak(ident: Ident): String? {
        val request = PostRequest(HentNyesteAktivSakRequest(personident = ident.identifikator))
        val nyesteSakUrl = url.resolve("arena/nyesteaktivesak")
        return client.post(nyesteSakUrl, request)
    }

    override fun opprettArenaOppgave(arenaOpprettetForespørsel: ArenaOpprettOppgaveForespørsel): ArenaOpprettOppgaveRespons {
        val request = PostRequest(arenaOpprettetForespørsel)
        val opprettArenaoppgaveUrl = url.resolve("arena/opprettoppgave")
        return client.post(opprettArenaoppgaveUrl, request) ?: error("Ingen respons fra Arena")
    }

    override fun behandleKjoerelisteOgOpprettOppgave(journalpostId: JournalpostId): String {
        val request = PostRequest(BehandleKjoerelisteOgOpprettOppgaveRequest(journalpostId.referanse.toString()))
        val behandleKjoerelisteOgOpprettOppgaveUrl = url.resolve("arena/behandleKjoerelisteOgOpprettOppgave")
        return client.post<BehandleKjoerelisteOgOpprettOppgaveRequest, BehandleKjoerelisteOgOpprettOppgaveResponse>(
            behandleKjoerelisteOgOpprettOppgaveUrl,
            request
        )?.arenaSakId ?: error("Ingen respons fra Arena")
    }

}

data class HentNyesteAktivSakRequest(val personident: String)

data class BehandleKjoerelisteOgOpprettOppgaveRequest(
    val journalpostId: String
)

data class BehandleKjoerelisteOgOpprettOppgaveResponse(
    val arenaSakId: String
)