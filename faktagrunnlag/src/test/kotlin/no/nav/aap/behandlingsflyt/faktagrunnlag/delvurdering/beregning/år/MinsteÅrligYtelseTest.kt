package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år

import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MinsteÅrligYtelseTest{

    @Test
    fun `produserer tidslinje for minste årlig ytelse`(){
        assertThat( MinsteÅrligYtelse.tilTidslinje() ).isEqualTo(
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
}