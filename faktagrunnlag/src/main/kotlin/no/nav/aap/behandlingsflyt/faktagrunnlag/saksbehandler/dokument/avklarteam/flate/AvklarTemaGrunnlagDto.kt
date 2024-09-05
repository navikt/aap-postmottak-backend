package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.avklarteam.flate

data class AvklarTemaVurderingDto(
    val skalTilAap: Boolean
)


data class AvklarTemaGrunnlagDto(
    val vurdering: AvklarTemaVurderingDto?,
    val dokumenter: List<Long>
)
