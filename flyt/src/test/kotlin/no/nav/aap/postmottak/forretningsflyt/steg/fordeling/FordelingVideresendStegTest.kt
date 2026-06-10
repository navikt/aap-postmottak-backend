package no.nav.aap.postmottak.forretningsflyt.steg.fordeling

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.arena.AapSystem
import no.nav.aap.fordeler.arena.ArenaVideresender
import no.nav.aap.fordeler.arena.AvklarFordelingRepository
import no.nav.aap.fordeler.arena.AvklarFordelingVurdering
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class FordelingVideresendStegTest {

    private val avklarFordelingRepository = mockk<AvklarFordelingRepository>(relaxed = true)
    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val flytJobbRepository = mockk<FlytJobbRepository>(relaxed = true)
    private val innkommendeJournalpostRepository = mockk<InnkommendeJournalpostRepository>(relaxed = true)
    private val arenaVideresender = mockk<ArenaVideresender>(relaxed = true)
    private val prometheus = SimpleMeterRegistry()

    private val steg = FordelingVideresendSteg(
        avklarFordelingRepository,
        behandlingRepository,
        flytJobbRepository,
        innkommendeJournalpostRepository,
        arenaVideresender,
        prometheus,
    )

    private val journalpostId = JournalpostId(1L)
    private val behandlingId = BehandlingId(1L)
    private val kontekst = FlytKontekst(journalpostId, behandlingId, TypeBehandling.Fordeling)

    @Test
    fun `KELVIN-vurdering oppretter Journalføring-behandling og enqueuer ProsesserBehandlingJobb`() {
        val journalføringBehandlingId = BehandlingId(2L)
        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns
            AvklarFordelingVurdering(AapSystem.KELVIN, "KELVIN", LocalDateTime.now())
        every { behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring) } returns journalføringBehandlingId

        steg.utfør(kontekst)

        verify { behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring) }
        verify {
            flytJobbRepository.leggTil(withArg {
                assertThat(it.type()).isEqualTo(ProsesserBehandlingJobbUtfører.type)
            })
        }
        assertThat(prometheus.counter("fordeling_videresend", "system", "kelvin").count()).isEqualTo(1.0)
    }

    @Test
    fun `ARENA-vurdering kaller arenaVideresender og inkrementerer prometheus-teller`() {
        val innkommendeId = 42L
        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns
            AvklarFordelingVurdering(AapSystem.ARENA, "KELVIN", LocalDateTime.now())
        every { innkommendeJournalpostRepository.hentId(journalpostId) } returns innkommendeId

        steg.utfør(kontekst)

        verify { arenaVideresender.videresendJournalpostTilArena(journalpostId, innkommendeId) }
        assertThat(prometheus.counter("fordeling_videresend", "system", "arena").count()).isEqualTo(1.0)
    }

    @Test
    fun `IGNORERT-vurdering videresender ikke og inkrementerer ikke prometheus-teller`() {
        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns
            AvklarFordelingVurdering(AapSystem.IGNORERT, "KELVIN", LocalDateTime.now())

        steg.utfør(kontekst)

        verify(exactly = 0) { behandlingRepository.opprettBehandling(any(), any()) }
        verify(exactly = 0) { arenaVideresender.videresendJournalpostTilArena(any(), any()) }
        assertThat(prometheus.find("fordeling_videresend").counter()).isNull()
    }
}
