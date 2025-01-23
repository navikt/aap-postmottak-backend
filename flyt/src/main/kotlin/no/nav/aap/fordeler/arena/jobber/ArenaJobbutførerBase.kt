package no.nav.aap.fordeler.arena.jobber

import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.Journalstatus
import org.slf4j.LoggerFactory

abstract class ArenaJobbutførerBase(protected open val journalpostService: JournalpostService): JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    final override fun utfør(input: JobbInput) {
        val journalpostId = input.getBaseKOntekst().journalpostId
        val journalpost = journalpostService.hentjournalpost(journalpostId)

        if (journalpost.status != Journalstatus.MOTTATT) {
            log.info("Journalpost ${journalpostId} har status ${journalpost.status}. Avbryer behandling.")
            return
        }

        utførArena(input)
    }

    protected abstract fun utførArena(input: JobbInput)



}