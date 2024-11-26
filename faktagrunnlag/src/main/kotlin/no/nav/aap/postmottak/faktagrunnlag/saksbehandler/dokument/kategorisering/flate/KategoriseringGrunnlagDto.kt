package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.flate

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.Brevkategori

data class KategoriseringVurderingDto(
    val brevkode: Brevkategori,
)

data class KategoriseringGrunnlagDto(
    val vurdering: KategoriseringVurderingDto?,
    val dokumenter: List<Long>
)
