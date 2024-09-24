package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.flate

import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode


data class KategoriseringVurderingDto(
    val brevkode: Brevkode,
)

data class KategoriseringGrunnlagDto(
    val vurdering: KategoriseringVurderingDto?,
    val dokumenter: List<Long>
)
