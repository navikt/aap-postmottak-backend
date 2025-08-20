package no.nav.aap.fordeler.arena.jobber

import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import org.slf4j.LoggerFactory

abstract class ArenaJobbutførerBase(val journalpostService: JournalpostService): JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    final override fun utfør(input: JobbInput) {
        val journalpostId = input.getBaseKontekst().journalpostId
        val journalpost = journalpostService.hentJournalpost(journalpostId)

        if (journalpost.status != Journalstatus.MOTTATT) {
            log.info("Journalpost $journalpostId har status ${journalpost.status}. Avbryter behandling.")
            return
        }

        utførArena(input, journalpost)
    }

    protected abstract fun utførArena(input: JobbInput, journalpost: Journalpost)

}
