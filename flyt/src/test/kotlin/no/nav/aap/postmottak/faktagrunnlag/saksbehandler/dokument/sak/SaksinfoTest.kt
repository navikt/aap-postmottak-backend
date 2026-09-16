package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak

import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SaksinfoTest {

    @Test
    fun `tillater automatisk behandling når det ikke finnes noen kelvin-saker fra før`() {
        assertTrue(emptyList<Saksinfo>().tillaterAutomatiskBehandlingAvLegeerklæring())
    }

    @Test
    fun `tillater automatisk behandling når det finnes en åpen sak, selv om alle avsluttede saker er avslag`() {
        val saker = listOf(
            sak(avslag = true, finnesÅpenBehandling = true),
        )
        assertTrue(saker.tillaterAutomatiskBehandlingAvLegeerklæring())
    }

    @Test
    fun `tillater automatisk behandling når det finnes en sak uten avslag`() {
        val saker = listOf(
            sak(avslag = false, finnesÅpenBehandling = false),
        )
        assertTrue(saker.tillaterAutomatiskBehandlingAvLegeerklæring())
    }

    @Test
    fun `tillater ikke automatisk behandling når alle saker er avslag og ingen åpen sak finnes`() {
        val saker = listOf(
            sak(avslag = true, finnesÅpenBehandling = false),
            sak(avslag = true, finnesÅpenBehandling = null),
        )
        assertFalse(saker.tillaterAutomatiskBehandlingAvLegeerklæring())
    }

    private fun sak(
        avslag: Boolean,
        finnesÅpenBehandling: Boolean?
    ) = Saksinfo(
        saksnummer = "42",
        periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2999, 12, 31)),
        avslag = avslag,
        resultat = null,
        finnesÅpenBehandling = finnesÅpenBehandling
    )
}
