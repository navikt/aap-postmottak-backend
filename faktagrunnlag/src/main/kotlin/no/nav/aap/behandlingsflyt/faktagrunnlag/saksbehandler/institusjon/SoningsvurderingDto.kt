package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import no.nav.aap.verdityper.dokument.JournalpostId

data class SoningsvurderingDto(
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val soningUtenforFengsel: Boolean,
    val begrunnelseForSoningUtenforAnstalt: String,
    val arbeidUtenforAnstalt: Boolean?,
    val begrunnelseForArbeidUtenforAnstalt: String?
)
