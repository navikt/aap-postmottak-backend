package no.nav.aap.domene.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
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
