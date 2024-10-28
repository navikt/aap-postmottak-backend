package no.nav.aap.postmottak.klient.gosysoppgave

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.slf4j.LoggerFactory
import java.net.URI


private val log = LoggerFactory.getLogger(Oppgaveklient::class.java)

class Oppgaveklient {

    private val url = URI.create(requiredConfigForKey("integrasjon.oppgaveapi.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.oppgaveapi.scope"),
    )
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    fun opprettOppgave(journalpostId: JournalpostId, ident: String) {
        val path = url.resolve("/api/v1/oppgaver")

        val request = PostRequest(OpprettOppgaveRequest(journalpostId = journalpostId.toString(), tilordnetRessurs = ident))

        try {
            client.post(path, request) {_, _ -> Unit}
        } catch (e: Exception) {
            log.warn("Feil mot oppgaveApi under opprettelse av oppgave: ${e.message}", e)
        }
    }

    fun finnOppgaverForJournalpost(journalpostId: JournalpostId): List<Long> {
        val path = url.resolve("/api/v1/oppgaver?journalpostId=$journalpostId&oppgavetype=$OPPGAVETYPE")

        return client.get<FinnOppgaverResponse>(path, GetRequest())?.oppgaver?.map { it.id } ?: emptyList()
    }

    fun ferdigstillOppgave(journalpostId: JournalpostId) {
        val path = url.resolve("/api/v1/oppgaver/$journalpostId")

        val request = PatchRequest(FerdigstillOppgaveRequest())

        try {
            client.patch(path, request) {_, _ -> Unit}
        } catch (e: Exception) {
            log.warn("Feil mot oppgaveApi under lukking av oppgave: ${e.message}", e)
        }
    }

}

data class FinnOppgaverResponse(
    val oppgaver: List<Oppgave>
)

data class Oppgave(
    val id: Long,
)