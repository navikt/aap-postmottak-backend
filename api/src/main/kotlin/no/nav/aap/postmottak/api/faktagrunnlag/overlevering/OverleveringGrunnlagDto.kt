package no.nav.aap.postmottak.api.faktagrunnlag.overlevering

data class OverleveringVurderingDto(
    val skalOverleveres: Boolean,
)

data class OverleveringGrunnlagDto(
    val vurdering: OverleveringVurderingDto?,
)
