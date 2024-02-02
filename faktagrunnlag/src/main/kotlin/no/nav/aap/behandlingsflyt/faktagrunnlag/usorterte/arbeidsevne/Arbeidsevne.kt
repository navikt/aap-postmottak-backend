package no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.arbeidsevne

import no.nav.aap.verdityper.Prosent

data class Arbeidsevne(
    val begrunnelse: String,
    val andelNedsattArbeidsevne: Prosent
)
