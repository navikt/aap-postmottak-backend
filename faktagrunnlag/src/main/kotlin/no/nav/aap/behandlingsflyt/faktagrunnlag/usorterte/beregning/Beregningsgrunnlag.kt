package no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.vilkårsresultat.Faktagrunnlag
import no.nav.aap.verdityper.GUnit

interface Beregningsgrunnlag {
    fun grunnlaget(): GUnit
    fun faktagrunnlag(): Faktagrunnlag
}
