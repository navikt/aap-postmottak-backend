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
    fun `tillater automatisk behandling når det finnes en åpen sak, selv om ingen saker har rett nå eller i fremtiden`() {
        val saker = listOf(
            sak(finnesÅpenBehandling = true, harRettNåEllerIFramtiden = null),
        )
        assertTrue(saker.tillaterAutomatiskBehandlingAvLegeerklæring())
    }

    @Test
    fun `tillater automatisk behandling når det finnes en sak med rett nå eller i fremtiden`() {
        val saker = listOf(
            sak(finnesÅpenBehandling = false, harRettNåEllerIFramtiden = true),
        )
        assertTrue(saker.tillaterAutomatiskBehandlingAvLegeerklæring())
    }

    @Test
    fun `tillater ikke automatisk behandling når ingen sak har rett nå eller i fremtiden og ingen åpen sak finnes`() {
        val saker = listOf(
            sak(finnesÅpenBehandling = false, harRettNåEllerIFramtiden = null),
            sak(finnesÅpenBehandling = null, harRettNåEllerIFramtiden = null),
        )
        assertFalse(saker.tillaterAutomatiskBehandlingAvLegeerklæring())
    }

    private fun sak(
        finnesÅpenBehandling: Boolean?,
        harRettNåEllerIFramtiden: Boolean?,
    ) = Saksinfo(
        saksnummer = "42",
        periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2999, 12, 31)),
        finnesÅpenBehandling = finnesÅpenBehandling,
        harRettNåEllerIFramtiden = harRettNåEllerIFramtiden,
    )
}
