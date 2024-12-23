package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.flate

data class AvklarTemaVurderingDto(
    val skalTilAap: Boolean
)

data class AvklarTemaGrunnlagDto(
    val vurdering: AvklarTemaVurderingDto?,
    val dokumenter: List<String>
)
