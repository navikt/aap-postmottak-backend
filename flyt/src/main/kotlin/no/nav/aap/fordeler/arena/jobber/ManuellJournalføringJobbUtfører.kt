package no.nav.aap.fordeler.arena.jobber

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.JournalføringsType
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.Oppgavetype
import no.nav.aap.postmottak.journalføringCounter
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.JournalpostMedDokumentTitler
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(ManuellJournalføringJobbUtfører::class.java)

class ManuellJournalføringJobbUtfører(
    private val gosysOppgaveGateway: GosysOppgaveGateway,
    journalpostService: JournalpostService,
    val prometheus: MeterRegistry = SimpleMeterRegistry()
) : ArenaJobbutførerBase(journalpostService) {

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            val gosysOppgaveGateway = GatewayProvider.provide(GosysOppgaveGateway::class)
            val journalpostService = JournalpostService.konstruer(connection)
            return ManuellJournalføringJobbUtfører(
                gosysOppgaveGateway,
                journalpostService,
                PrometheusProvider.prometheus
            )
        }

        override fun type() = "arena.manuell.journalføring"

        override fun navn() = "Manuell journalføring"

        override fun beskrivelse() = "Oppretter oppgave for manuell journalføring"

        override fun retries() = 6

    }

    override fun utførArena(input: JobbInput, journalpost: JournalpostMedDokumentTitler) {
        val kontekst = input.getArenaVideresenderKontekst()

        val eksisterendeOppgaver =
            gosysOppgaveGateway.finnOppgaverForJournalpost(
                kontekst.journalpostId,
                listOf(Oppgavetype.JOURNALFØRING, Oppgavetype.FORDELING)
            )

        /* midlertidig kode for å fikse fordelingsoppgaver. */
        val specialCase = listOf(
            698581045L,
            698581047L,
            698588247L,
            698590382L,
            698592673L,
            698598590L,
            698598613L,
            698598639L,
            698599024L,
            698600148L,
            698600159L,
            698600240L,
            698600312L,
        ).map { JournalpostId(it) }

        if (kontekst.journalpostId in specialCase) {
            log.info("special case ${kontekst.journalpostId}")
            val eksisterendeFordelingsOppgaver =
                gosysOppgaveGateway.finnOppgaverForJournalpost(
                    kontekst.journalpostId,
                    listOf(Oppgavetype.FORDELING)
                )
            eksisterendeFordelingsOppgaver.forEach {
                gosysOppgaveGateway.ferdigstillOppgave(it)
            }
            if (kontekst.navEnhet != null && input.antallRetriesForsøkt() < 3) {
                gosysOppgaveGateway.opprettJournalføringsOppgave(
                    kontekst.journalpostId,
                    kontekst.ident,
                    kontekst.hoveddokumenttittel,
                    kontekst.navEnhet
                )
                log.info("Opprettet journalføringsoppgave i gosys for ${kontekst.journalpostId}")
                prometheus.journalføringCounter(type = JournalføringsType.jfr, enhet = kontekst.navEnhet).increment()
            } else {
                gosysOppgaveGateway.opprettFordelingsOppgave(
                    kontekst.journalpostId,
                    kontekst.ident,
                    kontekst.hoveddokumenttittel
                )
                log.info("Forsøkt å opprette journalføringsoppgave for journalpost ${kontekst.journalpostId} for mange ganger - opprettet fordelingsoppgave")
                prometheus.journalføringCounter(type = JournalføringsType.fdr).increment()
            }
            return
        }

        if (eksisterendeOppgaver.isNotEmpty()) {
            log.info("Det finnes allerede en journalføringsoppgave for journalpost ${kontekst.journalpostId} - oppretter ingen ny")
        } else if (kontekst.navEnhet != null && input.antallRetriesForsøkt() < 3) {
            gosysOppgaveGateway.opprettJournalføringsOppgave(
                kontekst.journalpostId,
                kontekst.ident,
                kontekst.hoveddokumenttittel,
                kontekst.navEnhet
            )
            log.info("Opprettet journalføringsoppgave i gosys for ${kontekst.journalpostId}")
            prometheus.journalføringCounter(type = JournalføringsType.jfr, enhet = kontekst.navEnhet).increment()
        } else {
            gosysOppgaveGateway.opprettFordelingsOppgave(
                kontekst.journalpostId,
                kontekst.ident,
                kontekst.hoveddokumenttittel
            )
            log.info("Forsøkt å opprette journalføringsoppgave for journalpost ${kontekst.journalpostId} for mange ganger - opprettet fordelingsoppgave")
            prometheus.journalføringCounter(type = JournalføringsType.fdr).increment()
        }
    }

}