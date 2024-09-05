package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.kategorisering.flate

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode


data class KategoriseringVurderingDto(
    val brevkode: Brevkode,
)

data class KategoriseringGrunnlagDto(
    val vurdering: KategoriseringVurderingDto?,
    val dokumenter: List<Long>
)
