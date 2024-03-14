package no.nav.aap.tidslinje

import no.nav.aap.verdityper.Periode
import java.time.LocalDate
import java.time.Period
import java.util.*


class Tidslinje<T, X : Segment<T>>(initSegmenter: NavigableSet<X> = TreeSet()) : Iterable<Segment<T>> {

    constructor(initSegmenter: List<X>) : this(TreeSet(initSegmenter))
    constructor(verdi: X) : this(TreeSet(listOf(verdi)))

    private val segmenter: NavigableSet<X> = TreeSet()

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

    fun segmenter(): NavigableSet<X> {
        return TreeSet(segmenter)
    }

    fun perioder(): NavigableSet<Periode> {
        return TreeSet(segmenter.map { it.periode })
    }

    /**
     * Merge av to tidslinjer, prioriterer verdier fra den som merges over den som det kalles på
     * oppretter en tredje slik at orginale verdier bevares
     */
    fun <E, V, HØYRE : Segment<E>, RETUR : Segment<V>> kombiner(
        other: Tidslinje<E, HØYRE>,
        joinStyle: JoinStyle<T, E, V, RETUR>
    ): Tidslinje<V, RETUR> {
        if (this.segmenter.isEmpty()) {
            val joinStyle1: JoinStyle<T, E, V, RETUR> = joinStyle
            val nyeSegmenter = other.segmenter.mapNotNull { segment ->
                joinStyle1.kombiner(segment.periode, null, segment.verdi)
            }
            return Tidslinje(nyeSegmenter.toCollection(TreeSet()))
        }
        if (other.segmenter.isEmpty()) {
            val nyeSegmenter = this.segmenter.mapNotNull { segment ->
                joinStyle.kombiner(segment.periode, segment.verdi, null)
            }
            return Tidslinje(nyeSegmenter.toCollection(TreeSet()))
        }

        val periodeIterator = PeriodeIterator(
            perioder(),
            other.perioder()
        )

        val nySammensetning: NavigableSet<RETUR> = TreeSet()
        while (periodeIterator.hasNext()) {
            val periode = periodeIterator.next()

            val left = this.segmenter.firstOrNull { segment -> segment.periode.overlapper(periode) }?.verdi
            val right = other.segmenter.firstOrNull { segment -> segment.periode.overlapper(periode) }?.verdi

            val ll = joinStyle.kombiner(periode, left, right)
            if (ll != null) {
                nySammensetning.add(ll)
            }
        }

        return Tidslinje(nySammensetning)
    }

    fun disjoint(periode: Periode): Tidslinje<T, Segment<T>> {
        return kombiner<Any?, T, Segment<Any?>, Segment<T>>(
            Tidslinje(listOf(Segment(periode, null))),
            StandardSammenslåere.kunVenstre()
        )
    }

    fun <E, V, HØYRE : Segment<E>, RETUR : Segment<V>> disjoint(
        other: Tidslinje<E, HØYRE>,
        combinator: (Periode, T & Any) -> RETUR
    ): Tidslinje<V, RETUR> {
        return kombiner(other, JoinStyle.DISJOINT(combinator))
    }

    fun kryss(periode: Periode): Tidslinje<T, Segment<T>> {
        return kombiner(Tidslinje(Segment(periode, Unit)), StandardSammenslåere.kunVenstre())
    }

    fun kryss(other: Tidslinje<Any?, Segment<Any?>>): Tidslinje<T, Segment<T>> {
        return kombiner(other, StandardSammenslåere.kunVenstre())
    }

    /**
     * Komprimerer tidslinjen
     * - Slår sammen segmetner hvor verdien er identisk (benytter equals for sjekk)
     */
    fun komprimer(): Tidslinje<T, Segment<T>> {
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

    fun <R> mapValue(mapper: (T) -> R): Tidslinje<R, Segment<R>> {
        val newSegments: NavigableSet<Segment<R>> = TreeSet()
        segmenter.forEach { s ->
            newSegments.add(
                Segment(
                    s.periode,
                    mapper(s.verdi)
                )
            )
        }
        return Tidslinje(newSegments)
    }

    fun splittOppEtter(period: Period): Tidslinje<T, Segment<T>> {
        if (segmenter.isEmpty()) {
            return Tidslinje()
        }
        return splittOppEtter(minDato(), maxDato(), period)
    }

    fun splittOppEtter(startDato: LocalDate, period: Period): Tidslinje<T, Segment<T>> {
        if (segmenter.isEmpty()) {
            return Tidslinje()
        }
        return splittOppEtter(startDato, maxDato(), period)
    }

    /**
     * Knekker opp segmenterene i henhold til period fom startDato tom sluttDato
     */
    fun splittOppEtter(startDato: LocalDate, sluttDato: LocalDate, period: Period): Tidslinje<T, Segment<T>> {
        require(!(LocalDate.MIN == startDato || LocalDate.MAX == sluttDato || sluttDato.isBefore(startDato))) {
            String.format(
                "kan ikke periodisere tidslinjen mellom angitte datoer: [%s, %s]",
                startDato,
                sluttDato
            )
        }

        val segmenter: NavigableSet<Segment<T>> = TreeSet()

        val maxLocalDate: LocalDate = minOf(maxDato(), sluttDato)
        var dt = startDato
        while (!dt.isAfter(maxLocalDate)) {
            val nextDt = dt.plus(period)

            val nesteSegmenter: NavigableSet<Segment<T>> = kryss(Periode(dt, nextDt.minusDays(1))).segmenter
            segmenter.addAll(nesteSegmenter)
            dt = nextDt
        }
        return Tidslinje(segmenter)
    }

    fun <R> splittOppOgMapOmEtter(
        period: Period,
        mapper: (NavigableSet<Segment<T>>) -> NavigableSet<Segment<R>>
    ): Tidslinje<R, Segment<R>> {
        if (segmenter.isEmpty()) {
            return Tidslinje()
        }
        return splittOppOgMapOmEtter(minDato(), maxDato(), period, mapper)
    }

    /**
     * Knekker opp segmenterene i henhold til period fom startDato tom sluttDato
     */
    fun <R> splittOppOgMapOmEtter(
        startDato: LocalDate,
        sluttDato: LocalDate,
        period: Period,
        mapper: (NavigableSet<Segment<T>>) -> NavigableSet<Segment<R>>
    ): Tidslinje<R, Segment<R>> {
        require(!(LocalDate.MIN == startDato || LocalDate.MAX == sluttDato || sluttDato.isBefore(startDato))) {
            String.format(
                "kan ikke periodisere tidslinjen mellom angitte datoer: [%s, %s]",
                startDato,
                sluttDato
            )
        }

        val segmenter: NavigableSet<Segment<R>> = TreeSet()

        val maxLocalDate: LocalDate = minOf(maxDato(), sluttDato)
        var dt = startDato
        while (!dt.isAfter(maxLocalDate)) {
            val nextDt = dt.plus(period)

            val nesteSegmenter: NavigableSet<Segment<T>> = kryss(Periode(dt, nextDt.minusDays(1))).segmenter
            segmenter.addAll(mapper(nesteSegmenter))
            dt = nextDt
        }
        return Tidslinje(segmenter)
    }

    /**
     * Henter segmentet som inneholder datoen
     */
    fun segment(dato: LocalDate): Segment<T>? {
        return segmenter.firstOrNull { segment -> segment.inneholder(dato) }
    }

    override fun iterator(): Iterator<Segment<T>> {
        return segmenter.iterator()
    }

    fun minDato(): LocalDate {
        check(!segmenter.isEmpty()) {
            "Timeline is empty" //$NON-NLS-1$
        }
        return segmenter.first().fom()
    }

    fun maxDato(): LocalDate {
        check(!segmenter.isEmpty()) {
            "Timeline is empty" //$NON-NLS-1$
        }
        return segmenter.last().tom()
    }

    fun helePerioden(): Periode {
        return Periode(minDato(), maxDato())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tidslinje<*, Segment<*>>

        // Benytter hashset for å slippe rør med compareTo osv..
        return HashSet(segmenter) == HashSet(other.segmenter)
    }

    override fun hashCode(): Int {
        return segmenter.hashCode()
    }

    override fun toString(): String {
        return "Tidslinje(segmenter=$segmenter)"
    }
}
