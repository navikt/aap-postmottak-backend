package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak

import no.nav.aap.komponenter.type.Periode

data class Saksinfo(
    val saksnummer: String,
    val periode: Periode
)
