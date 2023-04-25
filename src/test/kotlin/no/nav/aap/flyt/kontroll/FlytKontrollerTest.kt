package no.nav.aap.flyt.kontroll

import org.junit.jupiter.api.Test

class FlytKontrollerTest {

    private val flytKontroller = FlytKontroller()

    @Test
    fun name() {
        val kontekst = FlytKontekst(1L, 1L)
        flytKontroller.prosesserBehandling(kontekst)

        val behandling = flytKontroller.behandliger.getValue(1L)

        behandling.løsAvklaringsbehov(no.nav.aap.domene.behandling.avklaringsbehov.Definisjon.AVKLAR_YRKESSKADE, "Begrunnelse", "meg")

        flytKontroller.prosesserBehandling(kontekst)
    }
}
