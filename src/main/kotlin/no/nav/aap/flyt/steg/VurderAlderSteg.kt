package no.nav.aap.flyt.steg

import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.Vilkårstype
import no.nav.aap.flyt.StegType

class VurderAlderSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)
        val aldersvilkåret = behandling.vilkårsresultat().finnVilkår(Vilkårstype.ALDERSVILKÅRET)

        return StegResultat()
    }

    override fun type() = StegType.VURDER_ALDER
}
