package no.nav.aap.domene.behandling

class Vilkår(
    val type: Vilkårstype
) {
    val vilkårsperiode: MutableSet<Vilkårsperiode> = mutableSetOf()
}
