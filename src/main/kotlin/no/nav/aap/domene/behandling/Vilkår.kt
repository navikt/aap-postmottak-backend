package no.nav.aap.domene.behandling

class Vilkår(
    val type: Vilkårstype
) {
    val vilkårsperiode: MutableSet<Vilkårsperiode> = mutableSetOf()

    fun leggTilVurdering(vilkårsperiode: Vilkårsperiode) {
        this.vilkårsperiode.add(vilkårsperiode)
        // TODO: Legg til overlappende constraint
    }

    override fun toString(): String {
        return "Vilkår(type=$type)"
    }
}
