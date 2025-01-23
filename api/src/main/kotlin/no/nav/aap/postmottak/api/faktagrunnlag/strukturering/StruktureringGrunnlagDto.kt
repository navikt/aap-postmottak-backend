package no.nav.aap.postmottak.api.faktagrunnlag.strukturering

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType

data class StruktureringVurderingDto(
    val strukturertDokumentJson: String,
)

data class StruktureringGrunnlagDto(
    val vurdering: StruktureringVurderingDto?,
    val kategori: InnsendingType,
    val dokumenter: List<Long>
)
