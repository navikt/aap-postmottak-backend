
package no.nav.aap.behandlingsflyt.flyt.vilkår.student

import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.flyt.vilkår.TomtBeslutningstre
import no.nav.aap.behandlingsflyt.flyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkår
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsperiode
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsresultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.VurderingsResultat

class Studentvilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<StudentFaktagrunnlag> {
    private val vilkår: Vilkår

    init {
        this.vilkår = vilkårsresultat.finnVilkår(Vilkårtype.STUDENTVILKÅRET)
    }

    override fun vurder(grunnlag: StudentFaktagrunnlag): VurderingsResultat {
        return lagre(
            grunnlag,
            VurderingsResultat(
                utfall = Utfall.OPPFYLT,
                avslagsårsak = null,
                innvilgelsesårsak = null,
                beslutningstre = TomtBeslutningstre()
            )
        )
    }

    private fun lagre(grunnlag: StudentFaktagrunnlag, vurderingsResultat: VurderingsResultat): VurderingsResultat {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                Periode(grunnlag.vurderingsdato, grunnlag.sisteDagMedMuligYtelse),
                vurderingsResultat.utfall,
                false,
                null,
                vurderingsResultat.innvilgelsesårsak,
                vurderingsResultat.avslagsårsak,
                grunnlag,
                vurderingsResultat.beslutningstre
            )
        )

        return vurderingsResultat
    }

}