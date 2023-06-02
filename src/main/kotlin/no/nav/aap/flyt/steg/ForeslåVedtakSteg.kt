package no.nav.aap.flyt.steg

import no.nav.aap.domene.behandling.Behandling
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.StegType

class ForeslåVedtakSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)

        if (harHattAvklaringsbehov(behandling)) {
            return StegResultat(listOf(Definisjon.FORESLÅ_VEDTAK))
        }

        return StegResultat() // DO NOTHING
    }

    private fun harHattAvklaringsbehov(behandling: Behandling) =
        behandling.avklaringsbehov().filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }.isNotEmpty()

    override fun type(): StegType {
        return StegType.FORESLÅ_VEDTAK
    }
}
