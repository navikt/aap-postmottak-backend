package no.nav.aap.domene.behandling

class Vilkårsresultat(
    private val vilkår: List<Vilkår>
) {
    fun finnVilkår(vilkårstype: Vilkårstype): Vilkår {
        return vilkår.first { it.type == vilkårstype }
    }
}
