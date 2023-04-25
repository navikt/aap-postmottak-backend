package no.nav.aap.flyt

import org.junit.jupiter.api.Test

class DefinisjonTest {

    @Test
    fun `Skal finne neste steg for førstegangsbehandling`() {
        val førstegangsbehandling = Definisjon.førstegangsbehandling

        val neste = førstegangsbehandling.neste(StegType.START_BEHANDLING)

        assert(neste == StegType.INNGANGSVILKÅR)
    }
}
