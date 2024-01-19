package no.nav.aap.behandlingsflyt.avklaringsbehov

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class DefinisjonTest {

    @Test
    fun `Skal validere OK for alle definisjoner`() {
        try {
            Definisjon.values()
        } catch (e: Exception) {
            fail(e)
        }
    }
}
