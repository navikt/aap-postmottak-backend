package no.nav.aap.postmottak.kontrakt.avklaringsbehov

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class AvklaringsbehovKodeTest {

    @Test
    fun `Skal hente ut kode basert på string`() {
        try {
            val kode = AvklaringsbehovKode.valueOf("1337")
            assertThat(kode).isEqualTo(AvklaringsbehovKode.`1337`)
        } catch (e: Exception) {
            fail(e)
        }
    }
}
