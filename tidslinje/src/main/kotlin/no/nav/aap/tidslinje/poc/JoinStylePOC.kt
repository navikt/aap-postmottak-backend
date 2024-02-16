package no.nav.aap.tidslinje.poc

import no.nav.aap.verdityper.Periode

internal class JoinStyleInnerPOC<T, ST, U, SU, V, SV>(
    private val kombinerer: (Periode, ST, SU) -> SV
)
        where ST : SegmentPOC<T>,
              SU : SegmentPOC<U>,
              SV : SegmentPOC<V> {
    internal fun accept(venstre: ST, høyre: SU) {}
    internal fun kombiner(periode: Periode, venstre: ST?, høyre: SU?): SV? {
        if (venstre == null || høyre == null) return null
        return this.kombinerer(periode, venstre, høyre)
    }
}

internal class JoinStylePOCOuter<T, ST, U, SU>
        where ST : SegmentPOC<T>,
              SU : SegmentPOC<U> {
    internal fun accept(venstre: ST?, høyre: SU?) {}
}

internal class JoinStylePOCLeft<T, ST, U, SU>
        where ST : SegmentPOC<T>,
              SU : SegmentPOC<U> {
    internal fun accept(venstre: ST, høyre: SU?) {}
}

internal class JoinStylePOCRight<T, ST, U, SU>
        where ST : SegmentPOC<T>,
              SU : SegmentPOC<U> {
    internal fun accept(venstre: ST?, høyre: SU) {}
}
