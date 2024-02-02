package no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.meldeplikt

import no.nav.aap.verdityper.Periode

data class Fritaksvurdering(
    val periode: Periode,
    val begrunnelse: String,
    val harFritak: Boolean
)
