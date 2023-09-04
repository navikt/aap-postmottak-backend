package no.nav.aap.domene.behandling

import no.nav.aap.domene.typer.Periode

class Vilkår(
    val type: Vilkårstype
) {
    private val vilkårsperioder: MutableSet<Vilkårsperiode> = mutableSetOf()

    fun vilkårsperioder(): List<Vilkårsperiode> {
        return this.vilkårsperioder.toList()
    }

    fun leggTilVurdering(vilkårsperiode: Vilkårsperiode) {
        this.vilkårsperioder.add(vilkårsperiode)
        // TODO: Legg til overlappende constraint
    }

    override fun toString(): String {
        return "Vilkår(type=$type)"
    }

    fun leggTilIkkeVurdertPeriode(rettighetsperiode: Periode) {
        this.vilkårsperioder.add(
            Vilkårsperiode(
                periode = rettighetsperiode,
                utfall = Utfall.IKKE_VURDERT,
                manuellVurdering = false,
                faktagrunnlag = null
            )
        )
    }

    fun harPerioderSomIkkeErVurdert(periodeTilVurdering: Set<Periode>): Boolean {
        return this.vilkårsperioder
            .filter { periode -> periodeTilVurdering.any { vp -> periode.periode.overlapper(vp) } }
            .any { periode -> periode.erIkkeVurdert() }

    }
}
