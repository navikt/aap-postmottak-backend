package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.verdityper.Prosent

data class Arbeidsevne(
    val begrunnelse: String,
    val andelNedsattArbeidsevne: Prosent
)
