package no.nav.aap.behandlingsflyt.underveis.tidslinje

import java.util.*

class Tidslinje<T>(initSegmenter: NavigableSet<Segment<T>>) {

    constructor(initSegmenter: List<Segment<T>>) : this(TreeSet(initSegmenter))

    private val segmenter: NavigableSet<Segment<T>> = TreeSet()

    init {
        segmenter.addAll(initSegmenter)
        // Sjekk etter overlapp
        validerIkkeOverlapp()
    }

    private fun validerIkkeOverlapp() {
        var last: Segment<T>? = null
        for (seg in segmenter) {
            if (last != null) {
                require(!seg.overlapper(last)) { String.format("Overlapp %s - %s", last, seg) }
            }
            last = seg
        }
    }

    fun segmenter(): List<Segment<T>> {
        return segmenter.toList()
    }

    /**
     * Merge av to tidslinjer, prioriterer verdier fra den som merges over den som det kalles på
     * oppretter en tredje slik at orginale verdier bevares
     */
    fun <E, V> kombiner(
        tidslinje: Tidslinje<E>,
        sammenslåer: SegmentSammenslåer<T, E, V>
    ): Tidslinje<V> {
        val nySammensetning: NavigableSet<Segment<V>> = TreeSet()

        for (segment in segmenter) {
            val element = sammenslåer.sammenslå(segment.periode, segment, null)
            if (element != null) {
                nySammensetning.add(element)
            }
        }
        for (segment in tidslinje.segmenter) {
            if (nySammensetning.any { vp -> vp.periode.overlapper(segment.periode) }) {
                // Overlapper og må justere innholdet i listen
                val skalHåndteres = nySammensetning
                    .filter { eksisterendeSegment -> eksisterendeSegment.periode.overlapper(segment.periode) }
                    .toSet()

                nySammensetning.removeIf { eksisterendeSegment -> eksisterendeSegment.periode.overlapper(segment.periode) }

                skalHåndteres.forEach { eksisterendeSegment ->
                    (eksisterendeSegment.splittEtter(segment) + segment.except(eksisterendeSegment)).forEach { periode ->
                        val left = if (eksisterendeSegment.periode.overlapper(periode)) {
                            segmenter.first { it.overlapper(eksisterendeSegment) }.tilpassetPeriode(periode)
                        } else {
                            null
                        }
                        val right = if (segment.periode.overlapper(periode)) {
                            segment.tilpassetPeriode(periode)
                        } else {
                            null
                        }
                        val element = sammenslåer.sammenslå(periode, left, right)
                        if (element != null) {
                            nySammensetning.add(element)
                        }
                    }
                }
            } else {
                val element = sammenslåer.sammenslå(segment.periode, null, segment)
                if (element != null) {
                    nySammensetning.add(element)
                }
            }
        }

        return Tidslinje(nySammensetning)
    }

    fun komprimer(): Tidslinje<T> {
        val compressedSegmenter: NavigableSet<Segment<T>> = TreeSet()
        segmenter.forEach { segment ->
            if (compressedSegmenter.isEmpty()) {
                compressedSegmenter.add(segment)
            } else {
                val nærliggendeSegment =
                    compressedSegmenter.firstOrNull { it.inntil(segment) && it.verdi == segment.verdi }
                if (nærliggendeSegment != null) {
                    val forlengetKopi = nærliggendeSegment.forlengetKopi(segment.periode)
                    compressedSegmenter.remove(nærliggendeSegment)
                    compressedSegmenter.add(forlengetKopi)
                } else {
                    compressedSegmenter.add(segment)
                }
            }
        }
        return Tidslinje(compressedSegmenter)
    }
}
