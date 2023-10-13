package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.bistand.BistandFaktagrunnlag
import no.nav.aap.behandlingsflyt.flyt.vilkår.bistand.Bistandsvilkåret
import no.nav.aap.behandlingsflyt.grunnlag.bistand.BistandsTjeneste

class VurderBistandsbehovSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)

        val periodeTilVurdering =
            PeriodeTilVurderingTjeneste.utled(behandling = behandling, vilkår = Vilkårtype.BISTANDSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {

            val bistandsGrunnlag = BistandsTjeneste.hentHvisEksisterer(behandling.id)

            if (bistandsGrunnlag != null) {
                for (periode in periodeTilVurdering) {
                    val grunnlag = BistandFaktagrunnlag(periode.fom, periode.tom, bistandsGrunnlag.vurdering!!)
                    Bistandsvilkåret(behandling.vilkårsresultat()).vurder(grunnlag = grunnlag)
                }
            }

            val vilkår = behandling.vilkårsresultat().finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

            if (vilkår.harPerioderSomIkkeErVurdert(periodeTilVurdering)) {
                return StegResultat(listOf(Definisjon.AVKLAR_BISTANDSBEHOV))
            }
        }

        return StegResultat()
    }

    override fun type(): StegType {
        return StegType.VURDER_BISTANDSBEHOV
    }
}
