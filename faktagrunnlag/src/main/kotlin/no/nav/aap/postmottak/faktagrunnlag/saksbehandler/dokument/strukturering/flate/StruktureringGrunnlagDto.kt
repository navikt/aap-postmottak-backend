package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.flate

import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode

data class StruktureringVurderingDto(
    val strukturertDokumentJson: String,
)

data class StruktureringGrunnlagDto(
    val vurdering: StruktureringVurderingDto?,
    val kategori: Brevkode,
    val dokumenter: List<Long>
)
