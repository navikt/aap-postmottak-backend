package no.nav.aap.tidslinje.poc

import no.nav.aap.verdityper.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class TidslinjePOCTest {
    @Test
    fun hei() {
        val t1: TidslinjePOC<Int, SegmentPOC<Int>> =
            TidslinjePOC(SegmentPOC(Periode(LocalDate.now(), LocalDate.now()), 1))
        val t2: TidslinjePOC<Int, SegmentPOC<Int>> =
            TidslinjePOC(SegmentPOC(Periode(LocalDate.now(), LocalDate.now()), 2))
        val t3: TidslinjePOC<Int?, SegmentPOC<Int?>> =
            TidslinjePOC(SegmentPOC(Periode(LocalDate.now(), LocalDate.now()), 3))

        val kombinerInner: TidslinjePOC<Int, SegmentPOC<Int>> =
            t1.kombiner(t2, JoinStyleInnerPOC { periode: Periode, venstre: SegmentPOC<Int>, høyre: SegmentPOC<Int> ->
                SegmentPOC(periode, venstre.verdi + høyre.verdi)
            })

        val kombinerInnerNull: TidslinjePOC<Int, SegmentPOC<Int>> =
            t1.kombiner(
                t3,
                JoinStyleInnerPOC { periode: Periode, venstre: SegmentPOC<Int>, høyre: SegmentPOC<Int?> ->
                if (høyre.verdi == null) return@JoinStyleInnerPOC SegmentPOC(periode, venstre.verdi)
                SegmentPOC(periode, venstre.verdi + høyre.verdi)
            })

        val kombinerOuter: TidslinjePOC<Int, SegmentPOC<Int>> =
            t1.kombiner(
                t2,
                JoinStylePOCOuter()
            ) { periode: Periode, venstre: SegmentPOC<Int>?, høyre: SegmentPOC<Int>? ->
                if (høyre?.verdi == null) return@kombiner venstre?.verdi?.let { SegmentPOC(periode, venstre.verdi) }
                venstre?.verdi?.plus(høyre.verdi)?.let { SegmentPOC(periode, it) }
            }

        val kombinerOuterNull: TidslinjePOC<Int, SegmentPOC<Int>> =
            t1.kombiner(
                t3,
                JoinStylePOCOuter()
            ) { periode: Periode, venstre: SegmentPOC<Int>?, høyre: SegmentPOC<Int?>? ->
                if (høyre?.verdi == null) return@kombiner venstre?.verdi?.let { SegmentPOC(periode, venstre.verdi) }
                venstre?.verdi?.plus(høyre.verdi)?.let { SegmentPOC(periode, it) }
            }

        val kombinerVenstre: TidslinjePOC<Int, SegmentPOC<Int>> =
            t1.kombiner(t2, JoinStylePOCLeft()) { periode: Periode, venstre: SegmentPOC<Int>, høyre: SegmentPOC<Int>? ->
                if (høyre?.verdi == null) return@kombiner SegmentPOC(periode, venstre.verdi)
                SegmentPOC(periode, venstre.verdi.plus(høyre.verdi))
            }

        val kombinerVenstreNull: TidslinjePOC<Int, SegmentPOC<Int>> =
            t1.kombiner(
                t3,
                JoinStylePOCLeft()
            ) { periode: Periode, venstre: SegmentPOC<Int>, høyre: SegmentPOC<Int?>? ->
                if (høyre?.verdi == null) return@kombiner SegmentPOC(periode, venstre.verdi)
                SegmentPOC(periode, venstre.verdi.plus(høyre.verdi))
            }

        val kombinerHøyre: TidslinjePOC<Int, SegmentPOC<Int>> =
            t1.kombiner(
                t2,
                JoinStylePOCRight()
            ) { periode: Periode, venstre: SegmentPOC<Int>?, høyre: SegmentPOC<Int> ->
                if (venstre?.verdi == null) return@kombiner SegmentPOC(periode, høyre.verdi)
                SegmentPOC(periode, venstre.verdi.plus(høyre.verdi))
            }

        val kombinerHøyreNull: TidslinjePOC<Int, SegmentPOC<Int>> =
            t3.kombiner(
                t2,
                JoinStylePOCRight()
            ) { periode: Periode, venstre: SegmentPOC<Int?>?, høyre: SegmentPOC<Int> ->
                if (venstre?.verdi == null) return@kombiner SegmentPOC(periode, høyre.verdi)
                SegmentPOC(periode, venstre.verdi.plus(høyre.verdi))
            }
    }
}
