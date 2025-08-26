package no.nav.aap.fordeler.regler

import no.nav.aap.postmottak.gateway.Uføre
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ErIkkeGradertUføreRegelTest {
    @Test
    fun `Person med gradert uføre skal ikke til Kelvin`() {
        val perioder = listOf<Uføre>()
        assertFalse(ErIkkeGradertUføreRegel().vurder(GradertUføreRegelInput(perioder)))
    }

    @Test
    fun `Person uten gradert uføre skal til Kelvin`() {
        val perioder = listOf<Uføre>()
        assertTrue(ErIkkeGradertUføreRegel().vurder(GradertUføreRegelInput(perioder)))
    }
}