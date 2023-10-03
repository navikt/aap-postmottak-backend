package no.nav.aap.behandlingsflyt.domene.vilkår.sykdom

import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.domene.behandling.Avslagsårsak
import no.nav.aap.behandlingsflyt.domene.behandling.TomtBeslutningstre
import no.nav.aap.behandlingsflyt.domene.behandling.Utfall
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkår
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkårsperiode
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkårsresultat
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkårstype
import no.nav.aap.behandlingsflyt.domene.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.domene.vilkår.VurderingsResultat

class Sykdomsvilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<SykdomsFaktagrunnlag> {
    private val vilkår: Vilkår

    init {
        this.vilkår = vilkårsresultat.finnVilkår(Vilkårstype.SYKDOMSVILKÅRET)
    }

    override fun vurder(grunnlag: SykdomsFaktagrunnlag): VurderingsResultat {
        val utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null

        val sykdomsvurdering = grunnlag.sykdomsvurdering

        if (sykdomsvurdering.erSkadeSykdomEllerLyteVesentligdel && sykdomsvurdering.erNedsettelseIArbeidsevneHøyereEnnNedreGrense == true) {
            utfall = Utfall.OPPFYLT
            // TODO: Legge til innvilget etter (f.eks YS)
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON // TODO noe mer rett
        }

        return lagre(grunnlag, VurderingsResultat(utfall = utfall, avslagsårsak, TomtBeslutningstre()))
    }

    private fun lagre(grunnlag: SykdomsFaktagrunnlag, vurderingsResultat: VurderingsResultat): VurderingsResultat {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                Periode(grunnlag.vurderingsdato, grunnlag.sisteDagMedMuligYtelse),
                vurderingsResultat.utfall,
                false,
                null,
                vurderingsResultat.avslagsårsak,
                grunnlag,
                vurderingsResultat.beslutningstre
            )
        )

        return vurderingsResultat
    }

}
