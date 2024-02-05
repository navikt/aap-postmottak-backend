package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år

import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Periode
import java.time.LocalDate

object MinsteÅrligYtelse {
    private val minsteÅrligYtelse = listOf(
        MinsteÅrligYtelseElement(2024, 7, 1, "2.041"),
        MinsteÅrligYtelseElement(LocalDate.MIN, "2"),
    )

    private class MinsteÅrligYtelseElement(
        private val dato:LocalDate,
        faktor: String
    ){
        constructor(år: Int, måned: Int, dag: Int, faktor: String) : this(LocalDate.of(år, måned, dag), faktor)

        private val faktor: GUnit =  GUnit(faktor)

        companion object{
            fun tilTidslinje():Tidslinje<GUnit>{
                return minsteÅrligYtelse
                    .reversed()
                    .zipWithNext { gjeldende, neste ->
                        val periode = Periode(gjeldende.dato, neste.dato.minusDays(1))
                        Segment(periode, gjeldende.faktor)
                    }
                    .plus(Segment(Periode(minsteÅrligYtelse.first().dato, LocalDate.MAX), minsteÅrligYtelse.first().faktor))
                    .let(::Tidslinje)
            }
        }
    }

    fun tilTidslinje(): Tidslinje<GUnit> {
        return MinsteÅrligYtelseElement.tilTidslinje()
    }
}