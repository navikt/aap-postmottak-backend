package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.GUnit

interface Beregningsgrunnlag {
    fun grunnlaget(): GUnit
}
