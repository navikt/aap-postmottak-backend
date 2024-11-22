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

    fun opprettEndreTemaOppgave(journalpostId: JournalpostId, personident: String) {
        log.info("Oppretter journalføringsoppgave for journalpost $journalpostId")

        return opprettOppgave(
            OpprettOppgaveRequest(
                oppgavetype = Oppgavetype.JFR.name,
                journalpostId = journalpostId.toString(),
                personident = personident,
                beskrivelse = "Et dokument med feil tema har dukket opp hos AAP. Kan du hjelpe dokumentet på veien til sin rette mottaker?"
            )
        )
    }

    fun opprettOppgave(oppgaveRequest: OpprettOppgaveRequest) {
        val path = url.resolve("/api/v1/oppgaver")

        val request = PostRequest(oppgaveRequest)

        try {
            client.post(path, request) { _, _ -> }
        } catch (e: Exception) {
            log.warn("Feil mot oppgaveApi under opprettelse av oppgave: ${e.message}", e)
        }
    }

    fun finnOppgaverForJournalpost(
        journalpostId: JournalpostId,
        oppgavetyper: List<Oppgavetype> = listOf(Oppgavetype.JFR)
    ): List<Long> {
        log.info("Finn oppgaver for journalpost: $journalpostId")
        val oppgaveparams = oppgavetyper.map { "&oppgavetype=${it.name}" }.joinToString(separator = "")
        val path =
            url.resolve("/api/v1/oppgaver?journalpostId=$journalpostId${oppgaveparams}&tema=AAP&statuskategori=AAPEN")

        return client.get<FinnOppgaverResponse>(path, GetRequest())?.oppgaver?.map { it.id } ?: emptyList()
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        log.info("Ferdigstiller oppgave $oppgaveId")
        val path = url.resolve("/api/v1/oppgaver/$oppgaveId")

        val request = PatchRequest(FerdigstillOppgaveRequest())

        try {
            client.patch(path, request) { _, _ -> }
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