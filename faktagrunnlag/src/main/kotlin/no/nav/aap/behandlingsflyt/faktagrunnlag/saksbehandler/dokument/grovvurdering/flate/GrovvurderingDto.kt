package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.grovvurdering.flate

data class GrovvurderingDto(
    val vurdering: Boolean?,
    val dokumenter: List<Long>
)
