package no.nav.aap.fordeler.arena.jobber

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
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
        
        if (kontekst.navEnhet != null && input.antallRetriesForsøkt() < 3) {
            gosysOppgaveGateway.opprettJournalføringsOppgaveHvisIkkeEksisterer(
                kontekst.journalpostId,
                kontekst.ident,
                kontekst.hoveddokumenttittel,
                kontekst.navEnhet
            )
        } else {
            gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(
                journalpostId = kontekst.journalpostId,
                personIdent = kontekst.ident,
                beskrivelse = kontekst.hoveddokumenttittel
            )
        }
    }

}