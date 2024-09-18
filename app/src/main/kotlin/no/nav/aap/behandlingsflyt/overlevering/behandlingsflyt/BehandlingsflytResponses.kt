package no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt

import no.nav.aap.komponenter.type.Periode

data class Saksinfo(
    val saksnummer: String,
    val periode: Periode
)
