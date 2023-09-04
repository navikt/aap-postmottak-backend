package no.nav.aap.flyt.steg

import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.Vilkårstype
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flate.behandling.periode.PeriodeTilVurderingTjeneste
import no.nav.aap.flyt.StegType

class VurderSykdomSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)

        val periodeTilVurdering =
            PeriodeTilVurderingTjeneste.utled(behandling = behandling, vilkår = Vilkårstype.ALDERSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val sykdomsvilkåret = behandling.vilkårsresultat().finnVilkår(Vilkårstype.SYKDOMSVILKÅRET)

            if (sykdomsvilkåret.harPerioderSomIkkeErVurdert(periodeTilVurdering)) {
                return StegResultat(listOf(Definisjon.AVKLAR_SYKDOM))
            }
        }
        return StegResultat()
    }

    override fun type(): StegType {
        return StegType.AVKLAR_SYKDOM
    }
}
