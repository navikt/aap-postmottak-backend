package no.nav.aap.fordeler.regler

import no.nav.aap.postmottak.gateway.GeografiskTilknytning
import no.nav.aap.postmottak.gateway.GeografiskTilknytningType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GeografiskTilknytningDtoRegelTest {
    @Test
    fun `Person i bydel skal til Kelvin hvis kommunen er godkjent`() {
        val geografiskTilknytning =
            GeografiskTilknytning(gtType = GeografiskTilknytningType.BYDEL, gtBydel = "030103", gtLand = null, gtKommune = null)
        val godkjenteGeografiskeTilknytninger = listOf(
            GeografiskTilknytning(gtType = GeografiskTilknytningType.KOMMUNE, gtKommune = "0301", gtBydel = null, gtLand = null),
        )
        assertTrue(
            GeografiskTilknytningRegel().vurder(
                GeografiskTilknytningRegelInput(
                    geografiskTilknytning,
                    godkjenteGeografiskeTilknytninger
                )
            )
        )
    }

    @Test
    fun `Person i kommune skal til Kelvin hvis kommunen er godkjent`() {
        val geografiskTilknytning =
            GeografiskTilknytning(gtType = GeografiskTilknytningType.KOMMUNE, gtBydel = null, gtLand = null, gtKommune = "0301")
        val godkjenteGeografiskeTilknytninger = listOf(
            GeografiskTilknytning(gtType = GeografiskTilknytningType.KOMMUNE, gtKommune = "0301", gtBydel = null, gtLand = null),
        )
        assertTrue(
            GeografiskTilknytningRegel().vurder(
                GeografiskTilknytningRegelInput(
                    geografiskTilknytning,
                    godkjenteGeografiskeTilknytninger
                )
            )
        )
    }
    
    @Test
    fun `Person i bydel skal ikke til Kelvin hvis kommunen ikke er godkjent`() {
        val geografiskTilknytning =
            GeografiskTilknytning(gtType = GeografiskTilknytningType.BYDEL, gtBydel = "030103", gtLand = null, gtKommune = null)
        val godkjenteGeografiskeTilknytninger = listOf(
            GeografiskTilknytning(gtType = GeografiskTilknytningType.KOMMUNE, gtKommune = "0302", gtBydel = null, gtLand = null),
        )
        assertFalse(
            GeografiskTilknytningRegel().vurder(
                GeografiskTilknytningRegelInput(
                    geografiskTilknytning,
                    godkjenteGeografiskeTilknytninger
                )
            )
        )
    }
    
    @Test
    fun `Person uten geografisk tilknytning skal ikke til Kelvin`() {
        val geografiskTilknytning = null
        val godkjenteGeografiskeTilknytninger = listOf(
            GeografiskTilknytning(gtType = GeografiskTilknytningType.KOMMUNE, gtKommune = "0301", gtBydel = null, gtLand = null),
        )
        assertFalse(
            GeografiskTilknytningRegel().vurder(
                GeografiskTilknytningRegelInput(
                    geografiskTilknytning,
                    godkjenteGeografiskeTilknytninger
                )
            )
        )
    }
}