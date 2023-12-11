package no.nav.aap.behandlingsflyt.beregning

class UføreBeregning(
    private val grunnlag: Beregningsgrunnlag,
    private val ytterligereNedsattGrunnlag: Beregningsgrunnlag,
    private val uføregrad: Prosent
) {

    init {
        require(uføregrad < Prosent.`100_PROSENT`) { "Uføregraden må være mindre enn 100 prosent" }
    }

    fun beregnUføre(): GrunnlagUføre {
        val oppjustertGrunnlagVedUføre = ytterligereNedsattGrunnlag.grunnlaget().dividert(uføregrad.kompliment())

        val høyesteGrunnlag = maxOf(grunnlag.grunnlaget(), oppjustertGrunnlagVedUføre)

        return GrunnlagUføre(
            grunnlaget = høyesteGrunnlag,
            grunnlag = grunnlag,
            grunnlagYtterligereNedsatt = ytterligereNedsattGrunnlag
        )
    }
}
