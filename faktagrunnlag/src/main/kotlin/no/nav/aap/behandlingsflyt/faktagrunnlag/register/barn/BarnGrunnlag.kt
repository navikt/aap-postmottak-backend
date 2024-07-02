package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.Ident

class BarnGrunnlag(val barn: List<Barn>) {

    fun tidslinje(): Tidslinje<Set<Ident>> {
        var tidslinje: Tidslinje<MutableSet<Ident>> = Tidslinje()
        barn.map { barnet ->
            Segment(
                verdi = barnet.ident, periode = barnet.periodeMedRettTil()
            )
        }.forEach { segment ->
            tidslinje = tidslinje.kombiner(Tidslinje(listOf(segment)), JoinStyle.CROSS_JOIN { periode, venstreSegment, høyreSegment ->
                val verdi : MutableSet<Ident> = venstreSegment?.verdi ?: mutableSetOf()
                if (høyreSegment?.verdi != null) {
                    verdi.add(høyreSegment.verdi)
                }
                Segment(periode, verdi)
            })
        }
        return tidslinje.mapValue { it.toSet() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BarnGrunnlag

        return barn == other.barn
    }

    override fun hashCode(): Int {
        return barn.hashCode()
    }
}
