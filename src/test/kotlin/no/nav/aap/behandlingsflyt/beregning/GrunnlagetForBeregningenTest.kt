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
}
