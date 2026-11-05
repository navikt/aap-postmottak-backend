package no.nav.aap.fordeler.regler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KelvinSakRegelTest {

    @Test
    fun `regelen er sann når personen har en Kelvin-sak`() {
        assertThat(KelvinSakRegel().vurder(KelvinSakRegelInput(listOf("saksnummer")))).isTrue()
    }

    @Test
    fun `regelen er usann når personen ikke har noen Kelvin-sak`() {
        assertThat(KelvinSakRegel().vurder(KelvinSakRegelInput(emptyList()))).isFalse()
    }
}
