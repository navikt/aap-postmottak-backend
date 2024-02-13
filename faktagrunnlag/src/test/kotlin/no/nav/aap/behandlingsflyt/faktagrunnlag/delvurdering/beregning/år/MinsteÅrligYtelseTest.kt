package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år

import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MinsteÅrligYtelseTest {

    @Test
    fun `produserer tidslinje for minste årlig ytelse`() {
        assertThat(MINSTE_ÅRLIG_YTELSE_TIDSLINJE).isEqualTo(
            Tidslinje(
                listOf(
                    Segment(
                        Periode(
                            LocalDate.MIN,
                            LocalDate.of(2024, 6, 30)
                        ),
                        GUnit(2)
                    ),
                    Segment(
                        Periode(
                            LocalDate.of(2024, 7, 1),
                            LocalDate.MAX
                        ),
                        GUnit("2.041")
                    )
                )
            )
        )
    }

    @Test
    fun `tidslinja er kontinuerlig`() {
        val perioder = MINSTE_ÅRLIG_YTELSE_TIDSLINJE.map { it.periode }

        assertThat(perioder.first().fom).isEqualTo(LocalDate.MIN)
        assertThat(perioder.last().tom).isEqualTo(LocalDate.MAX)

        assertThat(perioder.zipWithNext()).allMatch { (current, next) ->
            current.tom.plusDays(1) == next.fom
        }

    }

}