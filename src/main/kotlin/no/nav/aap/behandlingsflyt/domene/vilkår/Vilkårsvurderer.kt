package no.nav.aap.behandlingsflyt.domene.vilkår

import no.nav.aap.behandlingsflyt.domene.behandling.Faktagrunnlag

interface Vilkårsvurderer<T : Faktagrunnlag> {

    fun vurder(grunnlag: T): VurderingsResultat
}
