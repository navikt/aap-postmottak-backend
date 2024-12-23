package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak

import no.nav.aap.komponenter.type.Periode

data class Saksinfo(
    val saksnummer: String,
    val periode: Periode
)
