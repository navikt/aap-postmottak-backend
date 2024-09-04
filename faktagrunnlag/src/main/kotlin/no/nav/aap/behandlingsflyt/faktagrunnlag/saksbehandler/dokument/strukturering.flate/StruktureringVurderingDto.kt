package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.strukturering.flate

data class StruktureringVurderingDto(
    val vurdering: String?,
    val dokumenter: List<Long>
)
