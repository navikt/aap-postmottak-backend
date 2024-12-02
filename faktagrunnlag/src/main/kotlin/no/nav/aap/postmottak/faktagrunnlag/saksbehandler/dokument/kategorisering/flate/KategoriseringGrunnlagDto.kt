package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.flate

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType

data class KategoriseringVurderingDto(
    val brevkode: InnsendingType,
)

data class KategoriseringGrunnlagDto(
    val vurdering: KategoriseringVurderingDto?,
    val dokumenter: List<Long>
)
