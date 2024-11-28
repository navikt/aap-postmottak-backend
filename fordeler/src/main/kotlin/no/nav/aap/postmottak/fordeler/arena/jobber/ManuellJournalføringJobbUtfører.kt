package no.nav.aap.postmottak.fordeler.arena.jobber

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.klient.gosysoppgave.Oppgaveklient
import no.nav.aap.postmottak.klient.gosysoppgave.Oppgavetype
import no.nav.aap.postmottak.klient.gosysoppgave.OpprettOppgaveRequest
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(ManuellJournalføringJobbUtfører::class.java)

class ManuellJournalføringJobbUtfører(private val oppgaveklient: Oppgaveklient) : JobbUtfører {

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return ManuellJournalføringJobbUtfører(Oppgaveklient())
        }
        override fun type() = "arena.manuell.journalføring"

        override fun navn() = "Manuell journalføring"

        override fun beskrivelse() = "Oppretter oppgave for manuell journalføring"
        
        override fun retries() = 6

    }

    override fun utfør(input: JobbInput) {
        val kontekst = input.getArenaVideresenderKontekst()
        val eksisterendeOppgaver =
            oppgaveklient.finnOppgaverForJournalpost(
                kontekst.journalpostId,
                listOf(Oppgavetype.JOURNALFØRING, Oppgavetype.FORDELING)
            )

        if (eksisterendeOppgaver.isNotEmpty()) {
            log.info("Det finnes allerede en journalføringsoppgave for journalpost ${kontekst.journalpostId} - oppretter ingen ny")
        } else if (input.antallRetriesForsøkt() < 3) {
            oppgaveklient.opprettOppgave(journalføringsOppgave(kontekst))
            log.info("Opprettet journalføringsoppgave i gosys for ${kontekst.journalpostId}")
        } else {
            oppgaveklient.opprettOppgave(fordelingsOppgave(kontekst))
            log.info("Forsøkt å opprette journalføringsoppgave for journalpost ${kontekst.journalpostId} for mange ganger - opprettet fordelingsoppgave")
        }
    }

    private fun journalføringsOppgave(kontekst: ArenaVideresenderKontekst) =
        OpprettOppgaveRequest(
            oppgavetype = Oppgavetype.JOURNALFØRING.verdi,
            journalpostId = kontekst.journalpostId.toString(),
            personident = kontekst.ident.identifikator,
            beskrivelse = kontekst.hoveddokumenttittel,
            tildeltEnhetsnr = kontekst.navEnhet
        )

    private fun fordelingsOppgave(kontekst: ArenaVideresenderKontekst) =
        OpprettOppgaveRequest(
            oppgavetype = Oppgavetype.FORDELING.verdi,
            journalpostId = kontekst.journalpostId.toString(),
            personident = kontekst.ident.identifikator,
            beskrivelse = kontekst.hoveddokumenttittel
        )

}