package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.impl.GeneriskPlaceholderFlytSteg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BehandlingFlytTest {

    @Test
    fun `Skal finne neste steg for førstegangsbehandling`() {
        førstegangsbehandling.forberedFlyt(StegType.START_BEHANDLING)
        val neste = førstegangsbehandling.neste()

        assertThat(neste!!.type()).isEqualTo(StegType.VURDER_MEDLEMSKAP)
    }

    private val førstegangsbehandling = BehandlingFlytBuilder()
        .medSteg(GeneriskPlaceholderFlytSteg(StegType.START_BEHANDLING))
        .medSteg(GeneriskPlaceholderFlytSteg(StegType.VURDER_MEDLEMSKAP))
        .medSteg(GeneriskPlaceholderFlytSteg(StegType.FASTSETT_GRUNNLAG))
        .build()
}
