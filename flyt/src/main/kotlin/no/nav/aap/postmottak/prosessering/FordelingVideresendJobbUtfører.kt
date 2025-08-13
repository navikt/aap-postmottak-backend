package no.nav.aap.postmottak.prosessering

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.fordeler.RegelRepository
import no.nav.aap.fordeler.arena.ArenaVideresender
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon
import no.nav.aap.postmottak.Fagsystem
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.fordelingsCounter
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(FordelingVideresendJobbUtfører::class.java)

class FordelingVideresendJobbUtfører(
    val behandlingRepository: BehandlingRepository,
    val regelRepository: RegelRepository,
    val flytJobbRepository: FlytJobbRepository,
    val arenaVideresender: ArenaVideresender,
    val journalpostService: JournalpostService,
    val prometheus: MeterRegistry = SimpleMeterRegistry()
) : JobbUtfører {
    companion object : ProviderJobbSpesifikasjon {

        override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
            return FordelingVideresendJobbUtfører(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                ArenaVideresender.konstruer(repositoryProvider, GatewayProvider),
                JournalpostService.konstruer(repositoryProvider, GatewayProvider),
                PrometheusProvider.prometheus,
            )
        }

        override val type = "fordel.videresend"

        override val navn = "Prosesser videresending"

        override val beskrivelse = "Videresend journalpost"

    }

    override fun utfør(input: JobbInput) {
        val journalpostId = input.getJournalpostId()
        val innkommendeJournalpostId = input.getInnkommendeJournalpostId()
        val regelResultat = regelRepository.hentRegelresultat(journalpostId)
        requireNotNull(regelResultat) { "Fant ikke regelresultat for journalpostId=$journalpostId" }

        val safJournalpost = journalpostService.hentSafJournalpost(journalpostId)
        if (safJournalpost.journalstatus == Journalstatus.JOURNALFOERT) {
            log.info("Journalposten er allerede journalført i Joark. Ignorerer. JournalpostId: $journalpostId")
            return
        }

        if (regelResultat.skalTilKelvin()) {
            routeTilKelvin(journalpostId)
            prometheus.fordelingsCounter(Fagsystem.kelvin).increment()
        } else {
            arenaVideresender.videresendJournalpostTilArena(
                journalpostId,
                innkommendeJournalpostId = innkommendeJournalpostId
            )
            prometheus.fordelingsCounter(Fagsystem.arena).increment()
        }
    }

    private fun routeTilKelvin(journalpostId: JournalpostId) {
        log.info("Oppretter behandling for journalpost som skal til Kelvin: $journalpostId")
        val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
        flytJobbRepository.leggTil(
            JobbInput(ProsesserBehandlingJobbUtfører)
                .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
        )
    }
}