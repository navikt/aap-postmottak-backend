package no.nav.aap.behandlingsflyt.tilkjentytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.Tilkjent
import no.nav.aap.tidslinje.Segment
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.time.LocalDate

class BeregnTilkjentYtelseServiceTest {



    @Test
    fun beregnTilkjentYtelse() {
        val beregningsgrunnlag = Grunnlag11_19(
            GUnit(BigDecimal(4))
        )
        val underveisgrunnlag = UnderveisGrunnlag(
            id=1L, listOf(
                Underveisperiode(
                    periode = Periode(LocalDate.of(2024,2,8), LocalDate.of(2024,2,8)),
                    utfall = Utfall.OPPFYLT,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`100_PROSENT`,
                    gradering = null
                )
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(beregningsgrunnlag, underveisgrunnlag).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2024,2,8), LocalDate.of(2024,2,8)),
                verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260
                    gradering = Prosent.`0_PROSENT`
                )
            )
        )

    }
}