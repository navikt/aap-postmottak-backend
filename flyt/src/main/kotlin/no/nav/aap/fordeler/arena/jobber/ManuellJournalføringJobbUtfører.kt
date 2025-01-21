package no.nav.aap.fordeler.arena.jobber

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.Oppgavetype
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(ManuellJournalføringJobbUtfører::class.java)

class ManuellJournalføringJobbUtfører(
    private val gosysOppgaveGateway: GosysOppgaveGateway,
    private val journalpostGateway: JournalpostGateway
) : JobbUtfører {

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            val gosysOppgaveGateway = GatewayProvider.provide(GosysOppgaveGateway::class)
            val journalpostGateway = GatewayProvider.provide(JournalpostGateway::class)
            return ManuellJournalføringJobbUtfører(gosysOppgaveGateway, journalpostGateway)
        }

        override fun type() = "arena.manuell.journalføring"

        override fun navn() = "Manuell journalføring"

        override fun beskrivelse() = "Oppretter oppgave for manuell journalføring"

        override fun retries() = 6

    }

    override fun utfør(input: JobbInput) {
        val kontekst = input.getArenaVideresenderKontekst()
        val journalpostStatus = journalpostGateway.hentJournalpost(kontekst.journalpostId).journalstatus
        if (journalpostStatus == Journalstatus.JOURNALFOERT) {
            log.warn("Avbryter jobb, journalpost er allerede journalført")
            return
        }
        val eksisterendeOppgaver =
            gosysOppgaveGateway.finnOppgaverForJournalpost(
                kontekst.journalpostId,
                listOf(Oppgavetype.JOURNALFØRING, Oppgavetype.FORDELING)
            )
        if (eksisterendeOppgaver.isNotEmpty()) {
            log.info("Det finnes allerede en journalføringsoppgave for journalpost ${kontekst.journalpostId} - oppretter ingen ny")
        } else if (journalpostStatus == Journalstatus.FERDIGSTILT) {
            log.info("Journalpost ${kontekst.journalpostId} er allerede ferdigstilt - oppretter ingen journalføringsoppgave")
        } else if (kontekst.navEnhet != null && input.antallRetriesForsøkt() < 3) {
            gosysOppgaveGateway.opprettJournalføringsOppgave(
                kontekst.journalpostId,
                kontekst.ident,
                kontekst.hoveddokumenttittel,
                kontekst.navEnhet
            )
            log.info("Opprettet journalføringsoppgave i gosys for ${kontekst.journalpostId}")
        } else {
            gosysOppgaveGateway.opprettFordelingsOppgave(
                kontekst.journalpostId,
                kontekst.ident,
                kontekst.hoveddokumenttittel
            )
            log.info("Forsøkt å opprette journalføringsoppgave for journalpost ${kontekst.journalpostId} for mange ganger - opprettet fordelingsoppgave")
        }
    }

}