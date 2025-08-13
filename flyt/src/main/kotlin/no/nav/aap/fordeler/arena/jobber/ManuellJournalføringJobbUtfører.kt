package no.nav.aap.fordeler.arena.jobber

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost

class ManuellJournalføringJobbUtfører(
    private val gosysOppgaveGateway: GosysOppgaveGateway,
    journalpostService: JournalpostService,
    val prometheus: MeterRegistry = SimpleMeterRegistry()
) : ArenaJobbutførerBase(journalpostService) {

    companion object : ProviderJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
            val gosysOppgaveGateway = GatewayProvider.provide(GosysOppgaveGateway::class)
            val journalpostService = JournalpostService.konstruer(repositoryProvider, GatewayProvider)
            return ManuellJournalføringJobbUtfører(
                gosysOppgaveGateway,
                journalpostService,
                PrometheusProvider.prometheus
            )
        }

        override val type = "arena.manuell.journalføring"

        override val navn = "Manuell journalføring"

        override val beskrivelse = "Oppretter oppgave for manuell journalføring"

        override val retries = 6

    }

    override fun utførArena(input: JobbInput, journalpost: Journalpost) {
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