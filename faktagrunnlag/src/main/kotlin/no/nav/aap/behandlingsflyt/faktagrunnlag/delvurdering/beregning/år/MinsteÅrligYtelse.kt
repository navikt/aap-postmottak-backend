package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år

import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Periode
import java.time.LocalDate

val MINSTE_ÅRLIG_YTELSE_TIDSLINJE = Tidslinje(
    listOf(
        Segment(
            periode = Periode(LocalDate.MIN, LocalDate.of(2024, 6, 30)),
            verdi = GUnit("2")
        ),
        Segment(
            periode = Periode(LocalDate.of(2024, 7, 1), LocalDate.MAX),
            verdi = GUnit("2.041")
        )
    )
)
