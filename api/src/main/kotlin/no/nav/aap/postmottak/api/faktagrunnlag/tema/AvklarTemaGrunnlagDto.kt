package no.nav.aap.postmottak.api.faktagrunnlag.tema

import no.nav.aap.postmottak.under18.Under18VedBekreftetAap

data class AvklarTemaVurderingDto(
    val skalTilAap: Boolean,
    val tema: String? = null
)

data class AvklarTemaGrunnlagDto(
    val vurdering: AvklarTemaVurderingDto?,
    val dokumenter: List<String>,
    val journalpostMetadata: JournalpostMetadata,
    val under18VedBekreftetAap: Under18VedBekreftetAap?,
)

data class JournalpostMetadata(
    val brevkode: String?,
    val journalfoerendeEnhet: String?,
)
