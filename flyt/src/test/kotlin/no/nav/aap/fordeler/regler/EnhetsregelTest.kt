package no.nav.aap.fordeler.regler

import no.nav.aap.fordeler.EnhetMedOppfølgingsKontor
import no.nav.aap.postmottak.kontrakt.enhet.GodkjentEnhet
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class EnhetsregelTest {
    val NAV_MOSS = "0104"

    @ParameterizedTest
    @EnumSource(GodkjentEnhet::class)
    fun `Alle enheter i GodkjentEnhet-enum består enhetsregel`(enhet: GodkjentEnhet) {
        val input = EnhetsregelInput(
            enheter = EnhetMedOppfølgingsKontor(
                oppfølgingsenhet = null,
                norgEnhet = enhet.enhetNr))

        val resultat = Enhetsregel().vurder(input)
        assertTrue(resultat)
    }

    @Test
    fun `Oppfølgingskontor skal overstyre lokalkontor`() {
        val input = EnhetsregelInput(
            enheter = EnhetMedOppfølgingsKontor(
                oppfølgingsenhet = GodkjentEnhet.NAV_ASKER.enhetNr,
                norgEnhet = NAV_MOSS))
        
        val resultat = Enhetsregel().vurder(input)
        assertTrue(resultat)
    }

    @Test
    fun `Enhet som ikke ligger i lista over godkjente enheter blir avslått`() {
        val input = EnhetsregelInput(
            enheter = EnhetMedOppfølgingsKontor(
                oppfølgingsenhet = null,
                norgEnhet = NAV_MOSS))

        val resultat = Enhetsregel().vurder(input)
        assertFalse(resultat)
    }
}