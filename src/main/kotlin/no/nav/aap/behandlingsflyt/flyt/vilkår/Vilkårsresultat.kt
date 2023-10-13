package no.nav.aap.behandlingsflyt.flyt.vilkår

class Vilkårsresultat(
    private val vilkår: MutableList<Vilkår> = mutableListOf()
) {
    fun leggTilHvisIkkeEksisterer(vilkårtype: Vilkårtype): Vilkår {
        if (vilkår.none { it.type == vilkårtype }) {
            this.vilkår.add(Vilkår(vilkårtype))
        }
        return finnVilkår(vilkårtype)
    }

    fun finnVilkår(vilkårtype: Vilkårtype): Vilkår {
        return vilkår.first { it.type == vilkårtype }
    }

    fun alle(): List<Vilkår> {
        return vilkår.toList()
    }
}
