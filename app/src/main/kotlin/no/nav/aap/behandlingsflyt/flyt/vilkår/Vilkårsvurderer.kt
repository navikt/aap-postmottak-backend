package no.nav.aap.behandlingsflyt.flyt.vilkår

interface Vilkårsvurderer<T : Faktagrunnlag> {

    fun vurder(grunnlag: T): VurderingsResultat
}
