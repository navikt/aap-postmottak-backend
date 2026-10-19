package no.nav.aap.postmottak.avklaringsbehov.løser

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarTemaLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class AvklarTemaLøserTest {
    private val repository = mockk<AvklarTemaRepository>(relaxed = true)
    private val orkestrator = mockk<AvklaringsbehovOrkestrator>(relaxed = true)
    private val unleash = mockk<UnleashGateway>(relaxed = true)
    private val løser = AvklarTemaLøser(repository, orkestrator, unleash)
    private val behandlingId = BehandlingId(1L)
    private val kontekst = AvklaringsbehovKontekst(
        Bruker("SAKSBEHANDLER"),
        FlytKontekst(JournalpostId(1L), behandlingId, TypeBehandling.Journalføring)
    )

    @ParameterizedTest
    @EnumSource(Tema::class)
    fun `aktivert temavalg lagrer tema uten Gosys-vent`(tema: Tema) {
        every { unleash.isEnabled(PostmottakFeature.PostmottakVelgTema) } returns true
        løser.løs(kontekst, AvklarTemaLøsning(skalTilAap = tema == Tema.AAP, tema = tema))
        verify { repository.lagreTemaAvklaring(behandlingId, tema == Tema.AAP, tema) }
        verify { orkestrator.taAvVentPgaGosys(behandlingId) }
        verify(exactly = 0) { orkestrator.settBehandlingPåVentForTemaEndring(any()) }
    }

    @Test
    fun `uten toggle beholder nei UKJENT og Gosys-vent`() {
        løser.løs(kontekst, AvklarTemaLøsning(skalTilAap = false))
        verify { repository.lagreTemaAvklaring(behandlingId, false, Tema.UKJENT) }
        verify { orkestrator.settBehandlingPåVentForTemaEndring(behandlingId) }
    }

    @Test
    fun `uten toggle avvises temavalg før lagring`() {
        assertThrows<IllegalArgumentException> {
            løser.løs(kontekst, AvklarTemaLøsning(skalTilAap = false, tema = Tema.BAR))
        }
        verify(exactly = 0) { repository.lagreTemaAvklaring(any(), any(), any()) }
    }

    @Test
    fun `inkonsistent AAP-vurdering avvises før lagring`() {
        every { unleash.isEnabled(PostmottakFeature.PostmottakVelgTema) } returns true
        assertThrows<IllegalArgumentException> {
            løser.løs(kontekst, AvklarTemaLøsning(skalTilAap = true, tema = Tema.BAR))
        }
        assertThrows<IllegalArgumentException> {
            løser.løs(kontekst, AvklarTemaLøsning(skalTilAap = false, tema = Tema.AAP))
        }
        verify(exactly = 0) { repository.lagreTemaAvklaring(any(), any(), any()) }
    }
}
