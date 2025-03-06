package no.nav.aap.postmottak.prosessering

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.fordeler.RegelRepository
import no.nav.aap.fordeler.arena.ArenaVideresender
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.Fagsystem
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.fordelingsCounter
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
    val prometheus: MeterRegistry = SimpleMeterRegistry()
) : JobbUtfører {
    companion object : Jobb {

        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryProvider(connection)
            return FordelingVideresendJobbUtfører(
                repositoryProvider.provide(BehandlingRepository::class),
                repositoryProvider.provide(RegelRepository::class),
                FlytJobbRepository(connection),
                ArenaVideresender.konstruer(connection),
                PrometheusProvider.prometheus,
            )
        }

        override fun type() = "fordel.videresend"

        override fun navn() = "Prosesser videresending"

        override fun beskrivelse() = "Videresend journalpost"

    }

    override fun utfør(input: JobbInput) {
        val journalpostId = input.getJournalpostId()
        val regelResultat = regelRepository.hentRegelresultat(journalpostId)
        requireNotNull(regelResultat) { "Fant ikke regelresultat for journalpostId=$journalpostId" }
        
        if (regelResultat.skalTilKelvin()) {
            routeTilKelvin(journalpostId)
            prometheus.fordelingsCounter(Fagsystem.kelvin).increment()
        } else {
            arenaVideresender.videresendJournalpostTilArena(journalpostId)
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