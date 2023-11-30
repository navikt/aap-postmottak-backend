package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.Beløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.GUnit
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.adapter.InntektPerÅr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Year

class GrunnlagetForBeregningenTest {

    @Test
    fun `Hvis bruker ikke har inntekt beregnes grunnlaget til 0 kr`() {
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(emptyList())

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(GUnit(BigDecimal(0)))
    }

    @Test
    fun `Hvis bruker kun har inntekt siste kalenderår beregnes grunnlaget til inntekten dette året`() {
        val inntektPerÅr = listOf(InntektPerÅr(Year.of(2022), Beløp(BigDecimal(548920))))
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntektPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(GUnit(BigDecimal(5)))
    }

    @Test
    fun `Hvis bruker har vesentlig høyere inntekt i kroner siste kalenderår beregnes grunnlaget til inntekten det siste året`() {
        val inntektPerÅr = listOf(
            InntektPerÅr(Year.of(2022), Beløp(BigDecimal(5 * 109_784))),   // 548 920
            InntektPerÅr(Year.of(2021), Beløp(BigDecimal(2 * 104_716))),   // 209 432
            InntektPerÅr(Year.of(2020), Beløp(BigDecimal(2 * 100_853)))    // 201 706
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntektPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(GUnit(BigDecimal(5)))
    }

    @Test
    fun `Hvis bruker har samme inntekt i kroner siste tre kalenderår beregnes grunnlaget til gjennomsnittet av inntektene`() {
        val inntektPerÅr = listOf(
            InntektPerÅr(Year.of(2022), Beløp(BigDecimal(5 * 109_784))),   // 548 920
            InntektPerÅr(Year.of(2021), Beløp(BigDecimal(5 * 104_716))),   // 523 580
            InntektPerÅr(Year.of(2020), Beløp(BigDecimal(5 * 100_853)))    // 504 265
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntektPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(GUnit(BigDecimal(5)))
    }
}
