package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.strukturering.flate

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode

data class StruktureringVurderingDto(
    val strukturertDokumentJson: String,
)

data class StruktureringGrunnlagDto(
    val vurdering: StruktureringVurderingDto?,
    val kategori: Brevkode,
    val dokumenter: List<Long>
)
