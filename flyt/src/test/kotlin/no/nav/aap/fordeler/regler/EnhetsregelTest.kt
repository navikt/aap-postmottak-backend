package no.nav.aap.fordeler.regler

import no.nav.aap.fordeler.EnhetMedOppfølgingsKontor
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnhetsregelTest {
    val NAV_DRAMMEN = "0602"
    
    @Test
    fun `Oppfølgingskontor skal overstyre lokalkontor`() {
        val input = EnhetsregelInput(
            enheter = EnhetMedOppfølgingsKontor(
                oppfølgingsenhet = Enhet.NAV_ASKER.enhetNr,
                norgEnhet = NAV_DRAMMEN))
        
        val resultat = Enhetsregel().vurder(input)
        assertTrue(resultat)
    }
    
    @Test
    fun `Nav Asker er godkjent`() {
        val input = EnhetsregelInput(
            enheter = EnhetMedOppfølgingsKontor(
                oppfølgingsenhet = null,
                norgEnhet = Enhet.NAV_ASKER.enhetNr))
        
        val resultat = Enhetsregel().vurder(input)
        assertTrue(resultat)
    }
    
    @Test
    fun `SYFA Innlandet er godkjent`() {
        val input = EnhetsregelInput(
            enheter = EnhetMedOppfølgingsKontor(
                oppfølgingsenhet = null,
                norgEnhet = Enhet.SYFA_INNLANDET.enhetNr))
        
        val resultat = Enhetsregel().vurder(input)
        assertTrue(resultat)
    }

    @Test
    fun `Nav Drammen er ikke godkjent`() {
        val input = EnhetsregelInput(
            enheter = EnhetMedOppfølgingsKontor(
                oppfølgingsenhet = null,
                norgEnhet = NAV_DRAMMEN))

        val resultat = Enhetsregel().vurder(input)
        assertFalse(resultat)
    }
}