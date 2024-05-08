package no.nav.aap.behandlingsflyt.beregning.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
import no.nav.aap.verdityper.GUnit

class BeregningDTO (
    val beregningsGrunnlag: GUnit,
    val faktagrunnlag: Faktagrunnlag
)