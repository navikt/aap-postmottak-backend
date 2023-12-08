package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.adapter.InntektPerÅr
import java.util.*

class UføreBeregning(
    private val grunnlag: Beregningsgrunnlag,
    inntektPerÅrYtterligereNedsatt: List<InntektPerÅr>,
    private val uføregrad: Prosent,
    private val antattÅrligInntekt: InntektPerÅr,
    private val andelAvNedsettelsenSomSkyldesYrkesskaden: Prosent
) {
    private val inntektPerÅrYtterligereNedsatt: SortedSet<InntektPerÅr> =
        inntektPerÅrYtterligereNedsatt.toSortedSet().reversed()

    init {
        require(uføregrad < Prosent.`100_PROSENT`){"Uføregraden må være mindre enn 100 prosent"}
        require(this.inntektPerÅrYtterligereNedsatt.size == 3) { "Må oppgi tre inntekter" }
        require(
            this.inntektPerÅrYtterligereNedsatt.first().år == this.inntektPerÅrYtterligereNedsatt.last().år.plusYears(
                2
            )
        ) { "Inntektene må representere tre sammenhengende år" }
        require(this.inntektPerÅrYtterligereNedsatt.size == inntektPerÅrYtterligereNedsatt.size) { "Flere inntekter oppgitt for samme år" }
    }

    fun beregnUføre(): GrunnlagUføre {
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntektPerÅrYtterligereNedsatt.toList())
        val grunnlag11_19 = grunnlagetForBeregningen.beregnGrunnlaget()

        val yrkesskadeBeregning =
            YrkesskadeBeregning(grunnlag11_19, antattÅrligInntekt, andelAvNedsettelsenSomSkyldesYrkesskaden)
        val grunnlagYrkesskade = yrkesskadeBeregning.beregnYrkesskaden()

        val oppjustertGrunnlagVedUføre = grunnlagYrkesskade.grunnlaget().dividert(uføregrad.kompliment())

        val høyesteGrunnlag = maxOf(grunnlag11_19.grunnlaget(), oppjustertGrunnlagVedUføre)

        return GrunnlagUføre(
            grunnlaget = høyesteGrunnlag,
            grunnlag = grunnlag,
            grunnlagYtterligereNedsatt = grunnlagYrkesskade
        )
    }
}