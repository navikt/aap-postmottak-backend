package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.avklarteam.flate

data class AvklarTemaVurderingDto(
    val vurdering: Boolean?,
    val dokumenter: List<Long>
)
