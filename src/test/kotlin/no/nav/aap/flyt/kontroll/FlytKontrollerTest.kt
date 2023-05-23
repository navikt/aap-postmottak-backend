package no.nav.aap.flyt.kontroll

import no.nav.aap.avklaringsbehov.yrkesskade.AvklarYrkesskadeLøsning
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.Status
import org.junit.jupiter.api.Test

class FlytKontrollerTest {

    private val flytKontroller = FlytKontroller()

    @Test
    fun name() {
        val kontekst = FlytKontekst(1L, 1L)
        val behandling = BehandlingTjeneste.opprettBehandling(kontekst.fagsakId)
        flytKontroller.prosesserBehandling(kontekst)

        assert(behandling.status() == Status.UTREDES)
        assert(behandling.avklaringsbehov().isNotEmpty())

        flytKontroller.løsAvklaringsbehovOgFortsettProsessering(
            kontekst, avklaringsbehov = listOf(
                AvklarYrkesskadeLøsning("Begrunnelse", "meg")
            )
        )

        assert(behandling.status() == Status.AVSLUTTET)
    }
}
