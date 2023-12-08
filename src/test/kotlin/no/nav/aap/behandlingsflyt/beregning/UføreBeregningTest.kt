package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.Beløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.GUnit
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.adapter.InntektPerÅr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Year

class UføreBeregningTest {

    private companion object {
        private const val GRUNNBELØP_2022 = 109784
        private const val GRUNNBELØP_2021 = 104716
        private const val GRUNNBELØP_2020 = 100853
    }

    @Test
    fun `Det må oppgis tre inntekter for sammenhengende år, uten overlapp på år`() {
        val inntekterForToÅr = listOf(
            InntektPerÅr(Year.of(2022), Beløp(BigDecimal(0))),
            InntektPerÅr(Year.of(2021), Beløp(BigDecimal(0)))
        )
        val toÅrException = assertThrows<IllegalArgumentException> {
            UføreBeregning(
                grunnlag = Grunnlag11_19(GUnit(0)),
                inntektPerÅrYtterligereNedsatt = inntekterForToÅr,
                uføregrad = Prosent.`0_PROSENT`,
                antattÅrligInntekt = InntektPerÅr(2022, Beløp(0)),
                andelAvNedsettelsenSomSkyldesYrkesskaden = Prosent.`0_PROSENT`
            )
        }
        assertThat(toÅrException).hasMessage("Må oppgi tre inntekter")

        val inntekterForTreIkkesammenhengendeÅr = listOf(
            InntektPerÅr(Year.of(2022), Beløp(BigDecimal(0))),
            InntektPerÅr(Year.of(2021), Beløp(BigDecimal(0))),
            InntektPerÅr(Year.of(2019), Beløp(BigDecimal(0)))
        )
        val treIkkesammenhengendeÅrException = assertThrows<IllegalArgumentException> {
            UføreBeregning(
                grunnlag = Grunnlag11_19(GUnit(0)),
                inntektPerÅrYtterligereNedsatt = inntekterForTreIkkesammenhengendeÅr,
                uføregrad = Prosent.`0_PROSENT`,
                antattÅrligInntekt = InntektPerÅr(2022, Beløp(0)),
                andelAvNedsettelsenSomSkyldesYrkesskaden = Prosent.`0_PROSENT`
            )
        }
        assertThat(treIkkesammenhengendeÅrException).hasMessage("Inntektene må representere tre sammenhengende år")

        val inntekterForFlereInntekterPåSammeÅr = listOf(
            InntektPerÅr(Year.of(2022), Beløp(BigDecimal(0))),
            InntektPerÅr(Year.of(2021), Beløp(BigDecimal(0))),
            InntektPerÅr(Year.of(2021), Beløp(BigDecimal(0))),
            InntektPerÅr(Year.of(2020), Beløp(BigDecimal(0)))
        )
        val flereInntekterPåSammeÅrException = assertThrows<IllegalArgumentException> {
            UføreBeregning(
                grunnlag = Grunnlag11_19(GUnit(0)),
                inntektPerÅrYtterligereNedsatt = inntekterForFlereInntekterPåSammeÅr,
                uføregrad = Prosent.`0_PROSENT`,
                antattÅrligInntekt = InntektPerÅr(2022, Beløp(0)),
                andelAvNedsettelsenSomSkyldesYrkesskaden = Prosent.`0_PROSENT`
            )
        }
        assertThat(flereInntekterPåSammeÅrException).hasMessage("Flere inntekter oppgitt for samme år")
    }

    @Test
    fun `Hvis uføregraden er 0 prosent, endres ikke grunnlaget`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = Grunnlag11_19(GUnit(4)),
            inntektPerÅrYtterligereNedsatt = listOf(
                InntektPerÅr(2022, Beløp(6 * GRUNNBELØP_2022)),
                InntektPerÅr(2021, Beløp(6 * GRUNNBELØP_2021)),
                InntektPerÅr(2020, Beløp(6 * GRUNNBELØP_2020))
            ),
            uføregrad = Prosent.`0_PROSENT`,
            antattÅrligInntekt = InntektPerÅr(2022, Beløp(0)),
            andelAvNedsettelsenSomSkyldesYrkesskaden = Prosent.`0_PROSENT`
        )

        val grunnlagUføre = uføreBeregning.beregnUføre()

        assertThat(grunnlagUføre.grunnlaget()).isEqualTo(GUnit(6))
    }

    @Test
    fun `Hvis bruker har en uføregrad på 100 prosent, skal ikke uføreberegningen gjøres`() {
        assertThrows<IllegalArgumentException> {
            UføreBeregning(
                grunnlag = Grunnlag11_19(GUnit(4)),
                inntektPerÅrYtterligereNedsatt = listOf(
                    InntektPerÅr(2022, Beløp(0)),
                    InntektPerÅr(2021, Beløp(0)),
                    InntektPerÅr(2020, Beløp(0))
                ),
                uføregrad = Prosent.`100_PROSENT`,
                antattÅrligInntekt = InntektPerÅr(2022, Beløp(0)),
                andelAvNedsettelsenSomSkyldesYrkesskaden = Prosent.`0_PROSENT`
            )
        }
    }

    @Test
    fun `Hvis bruker hadde høyere inntekt ved ytterligere nedsatt, justert for uføregrad, brukes inntekter fra ytteligere nedsatt`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = Grunnlag11_19(GUnit(4)),
            inntektPerÅrYtterligereNedsatt = listOf(
                InntektPerÅr(2022, Beløp(3 * GRUNNBELØP_2022)),
                InntektPerÅr(2021, Beløp(3 * GRUNNBELØP_2021)),
                InntektPerÅr(2020, Beløp(3 * GRUNNBELØP_2020))
            ),
            uføregrad = Prosent.`50_PROSENT`,
            antattÅrligInntekt = InntektPerÅr(2022, Beløp(0)),
            andelAvNedsettelsenSomSkyldesYrkesskaden = Prosent.`0_PROSENT`
        )

        val grunnlagUføre = uføreBeregning.beregnUføre()

        assertThat(grunnlagUføre.grunnlaget()).isEqualTo(GUnit(6))
    }
}
