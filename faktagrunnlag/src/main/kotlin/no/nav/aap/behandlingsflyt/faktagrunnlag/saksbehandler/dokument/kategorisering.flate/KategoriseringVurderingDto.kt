package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.grovvurdering.flate

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode

data class KategoriseringVurderingDto(
    val vurdering: Brevkode?,
    val dokumenter: List<Long>
)
