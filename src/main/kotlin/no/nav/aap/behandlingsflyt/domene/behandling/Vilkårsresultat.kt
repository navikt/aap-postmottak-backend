package no.nav.aap.behandlingsflyt.domene.behandling

class Vilkårsresultat(
    private val vilkår: MutableList<Vilkår> = mutableListOf()
) {
    fun leggTilHvisIkkeEksisterer(vilkårstype: Vilkårstype): Vilkår {
        if (vilkår.none { it.type == vilkårstype }) {
            this.vilkår.add(Vilkår(vilkårstype))
        }
        return finnVilkår(vilkårstype)
    }

    fun finnVilkår(vilkårstype: Vilkårstype): Vilkår {
        return vilkår.first { it.type == vilkårstype }
    }

    fun alle(): List<Vilkår> {
        return vilkår.toList()
    }
}
