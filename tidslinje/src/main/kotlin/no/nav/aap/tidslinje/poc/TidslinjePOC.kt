package no.nav.aap.tidslinje.poc

import no.nav.aap.verdityper.Periode
import java.util.*

internal class TidslinjePOC<T, ST : SegmentPOC<T>>(
    private val segmenter: NavigableSet<ST>
) {
    internal constructor(vararg segmenter: ST) : this(TreeSet(segmenter.toSet()))

    internal fun <U, SU : SegmentPOC<U>, V, SV : SegmentPOC<V>> kombiner(
        høyre: TidslinjePOC<U, SU>,
        joinStyle: JoinStyleInnerPOC<T, ST, U, SU, V, SV>
    ): TidslinjePOC<V, SV> {
        return this.segmenter
            .zip(høyre.segmenter)
            .mapNotNull { (t, u) -> joinStyle.kombiner(t.periode, t, u) }
            .let { TidslinjePOC(TreeSet(it)) }
    }

    internal fun <U, SU : SegmentPOC<U>, V, SV : SegmentPOC<V>> kombiner(
        høyre: TidslinjePOC<U, SU>,
        joinStyle: JoinStylePOCOuter<T, ST, U, SU>,
        kombinerer: (Periode, ST?, SU?) -> SV?
    ): TidslinjePOC<V, SV> {
        return this.segmenter
            .zip(høyre.segmenter)
            .mapNotNull { (t, u) -> kombinerer(t.periode, t, u) }
            .let { TidslinjePOC(TreeSet(it)) }
    }

    internal fun <U, SU : SegmentPOC<U>, V, SV : SegmentPOC<V>> kombiner(
        høyre: TidslinjePOC<U, SU>,
        joinStyle: JoinStylePOCLeft<T, ST, U, SU>,
        kombinerer: (Periode, ST, SU?) -> SV
    ): TidslinjePOC<V, SV> {
        return this.segmenter
            .zip(høyre.segmenter)
            .map { (t, u) -> kombinerer(t.periode, t, u) }
            .let { TidslinjePOC(TreeSet(it)) }
    }

    internal fun <U, SU : SegmentPOC<U>, V, SV : SegmentPOC<V>> kombiner(
        høyre: TidslinjePOC<U, SU>,
        joinStyle: JoinStylePOCRight<T, ST, U, SU>,
        kombinerer: (Periode, ST?, SU) -> SV
    ): TidslinjePOC<V, SV> {
        return this.segmenter
            .zip(høyre.segmenter)
            .map { (t, u) -> kombinerer(t.periode, t, u) }
            .let { TidslinjePOC(TreeSet(it)) }
    }
}
