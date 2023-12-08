package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.GUnit

class GrunnlagUføre(
    private val grunnlaget: GUnit,
    private val grunnlag: Beregningsgrunnlag,
    private val grunnlagYtterligereNedsatt: Beregningsgrunnlag
) : Beregningsgrunnlag {

    override fun grunnlaget(): GUnit {
        return grunnlaget
    }
}
