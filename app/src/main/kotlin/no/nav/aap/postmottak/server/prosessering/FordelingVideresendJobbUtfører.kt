package no.nav.aap.postmottak.server.prosessering

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.fordeler.RegelRepository
import no.nav.aap.postmottak.fordeler.arena.ArenaVideresender
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(FordelingVideresendJobbUtfører::class.java)

class FordelingVideresendJobbUtfører(
    val behandlingRepository: BehandlingRepository,
    val regelRepository: RegelRepository,
    val flytJobbRepository: FlytJobbRepository,
    val arenaVideresender: ArenaVideresender,
    val prometheus: MeterRegistry = SimpleMeterRegistry()
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val journalpostId = input.getJournalpostId()
        val regelResultat = regelRepository.hentRegelresultat(journalpostId.referanse)
        if (regelResultat.skalTilKelvin()) {
            prometheus.counter("fordeling.videresend", "system", "kelvin").increment()
            routeTilKelvin(journalpostId)
        } else {
            prometheus.counter("fordeling.videresend", "system", "arena").increment()
            arenaVideresender.videresendJournalpostTilArena(journalpostId)
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

class FordelingVideresendJobb(
    val prometheus: MeterRegistry = SimpleMeterRegistry()
) : Jobb {

    override fun konstruer(connection: DBConnection): JobbUtfører {
        return FordelingVideresendJobbUtfører(
            BehandlingRepositoryImpl(connection),
            RegelRepository(connection),
            FlytJobbRepository(connection),
            ArenaVideresender.konstruer(connection),
            prometheus,
        )
    }

    override fun type() = "fordel.videresend"

    override fun navn() = "Prosesser videresending"

    override fun beskrivelse() = "Videresend journalpost"

}