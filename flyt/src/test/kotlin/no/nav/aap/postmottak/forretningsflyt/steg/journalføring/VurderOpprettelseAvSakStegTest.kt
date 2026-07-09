package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.aap.fordeler.Fordelingsutfall
import no.nav.aap.fordeler.RegelRepository
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VurderOpprettelseAvSakStegTest {

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    private val journalpostRepository = mockk<JournalpostRepository>()
    private val regelRepository = mockk<RegelRepository>(relaxed = true)
    private val avklaringsbehovService = mockk<AvklaringsbehovService>(relaxed = true)

    private val steg = VurderOpprettelseAvSakSteg(
        journalpostRepository,
        regelRepository,
        avklaringsbehovService,
    )

    @Test
    fun `oppdaterer avklaringsbehov for opprettelse av sak og fullfører steget`() {
        val journalpost = mockk<Journalpost>(relaxed = true)
        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        val resultat = steg.utfør(mockk(relaxed = true))

        verify(exactly = 1) {
            avklaringsbehovService.oppdaterAvklaringsbehov(
                definisjon = Definisjon.VURDER_OPPRETTELSE_AV_SAK,
                vedtakBehøverVurdering = any(),
                erTilstrekkeligVurdert = any(),
                kontekst = any(),
            )
        }
        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Test
    fun `krever manuell vurdering når fordeling tilsier manuell vurdering`() {
        val journalpost = mockk<Journalpost>(relaxed = true)
        every { journalpost.erUgyldig() } returns false
        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost
        every { regelRepository.hentRegelresultat(any<JournalpostId>()) } returns
            mockk { every { fordelingsutfall() } returns Fordelingsutfall.MANUELL }

        val vedtakBehøverVurdering = fangVedtakBehøverVurdering()

        steg.utfør(mockk(relaxed = true))

        assertThat(vedtakBehøverVurdering.captured.invoke()).isTrue()
    }

    @Test
    fun `krever ikke manuell vurdering når fordeling tilsier Kelvin`() {
        val journalpost = mockk<Journalpost>(relaxed = true)
        every { journalpost.erUgyldig() } returns false
        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost
        every { regelRepository.hentRegelresultat(any<JournalpostId>()) } returns
            mockk { every { fordelingsutfall() } returns Fordelingsutfall.KELVIN }

        val vedtakBehøverVurdering = fangVedtakBehøverVurdering()

        steg.utfør(mockk(relaxed = true))

        assertThat(vedtakBehøverVurdering.captured.invoke()).isFalse()
    }

    @Test
    fun `krever ikke manuell vurdering når regelresultat mangler`() {
        val journalpost = mockk<Journalpost>(relaxed = true)
        every { journalpost.erUgyldig() } returns false
        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost
        every { regelRepository.hentRegelresultat(any<JournalpostId>()) } returns null

        val vedtakBehøverVurdering = fangVedtakBehøverVurdering()

        steg.utfør(mockk(relaxed = true))

        assertThat(vedtakBehøverVurdering.captured.invoke()).isFalse()
    }

    @Test
    fun `krever ikke manuell vurdering når journalposten er ugyldig`() {
        val journalpost = mockk<Journalpost>(relaxed = true)
        every { journalpost.erUgyldig() } returns true
        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        val vedtakBehøverVurdering = fangVedtakBehøverVurdering()

        steg.utfør(mockk(relaxed = true))

        assertThat(vedtakBehøverVurdering.captured.invoke()).isFalse()
    }

    private fun fangVedtakBehøverVurdering(): CapturingSlot<() -> Boolean> {
        val vedtakBehøverVurdering = slot<() -> Boolean>()
        every {
            avklaringsbehovService.oppdaterAvklaringsbehov(
                definisjon = any(),
                vedtakBehøverVurdering = capture(vedtakBehøverVurdering),
                erTilstrekkeligVurdert = any(),
                kontekst = any(),
            )
        } just Runs
        return vedtakBehøverVurdering
    }
}
