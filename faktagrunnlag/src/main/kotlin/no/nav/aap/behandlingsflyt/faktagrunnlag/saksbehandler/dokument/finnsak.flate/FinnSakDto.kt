package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.finnsak.flate



data class FinnSakVurderingDto(
    val saksnummer: String,
)

data class FinnSakGrunnlagDto(
    val vurdering: FinnSakVurderingDto?,
)
