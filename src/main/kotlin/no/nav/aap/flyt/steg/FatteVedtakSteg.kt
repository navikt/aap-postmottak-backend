package no.nav.aap.flyt.steg

import no.nav.aap.domene.behandling.Behandling
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.StegType

class FatteVedtakSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)

        if (harHattAvklaringsbehovSomHarKrevdToTrinn(behandling)) {
            return StegResultat(listOf(Definisjon.FATTE_VEDTAK))
        }

        return StegResultat() // DO NOTHING
    }

    private fun harHattAvklaringsbehovSomHarKrevdToTrinn(behandling: Behandling) =
        behandling.avklaringsbehov()
            .filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
            .any { avklaringsbehov -> avklaringsbehov.erTotrinn() }

    override fun type(): StegType {
        return StegType.FATTE_VEDTAK
    }
}
