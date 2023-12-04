package no.nav.aap.behandlingsflyt.avklaringsbehov.arbeidsevne

import no.nav.aap.behandlingsflyt.beregning.Prosent

data class Arbeidsevne(
    val begrunnelse: String,
    val andelNedsattArbeidsevne: Prosent
)
