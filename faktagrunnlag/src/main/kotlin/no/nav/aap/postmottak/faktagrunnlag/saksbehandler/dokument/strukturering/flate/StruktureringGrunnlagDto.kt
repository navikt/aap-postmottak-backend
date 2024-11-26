package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.flate

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.Brevkategori

data class StruktureringVurderingDto(
    val strukturertDokumentJson: String,
)

data class StruktureringGrunnlagDto(
    val vurdering: StruktureringVurderingDto?,
    val kategori: Brevkategori,
    val dokumenter: List<Long>
)
