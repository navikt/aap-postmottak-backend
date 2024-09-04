package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.grovvurdering.flate

import java.io.InputStream

data class GrovvurderingDto(
    val vurdering: Boolean?,
    val dokumenter: List<InputStream>
)
