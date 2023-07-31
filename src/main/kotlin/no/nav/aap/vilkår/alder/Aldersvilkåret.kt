package no.nav.aap.vilkår.alder

import no.nav.aap.domene.behandling.TomtBeslutningstre
import no.nav.aap.domene.behandling.Utfall
import no.nav.aap.vilkår.Vilkårsvurderer
import no.nav.aap.vilkår.VurderingsResultat

object Aldersvilkåret : Vilkårsvurderer<Aldersgrunnlag> {
    override fun vurder(grunnlag: Aldersgrunnlag): VurderingsResultat {
        return VurderingsResultat(Utfall.OPPFYLT, TomtBeslutningstre())
    }

}
