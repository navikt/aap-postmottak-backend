package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import no.nav.aap.verdityper.dokument.JournalpostId

data class Soningsvurdering(
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val soningUtenforFengsel: Boolean,
    val begrunnelse: String? = null,
    val arbeidUtenforAnstalt: Boolean? = null,
)
