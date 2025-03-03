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