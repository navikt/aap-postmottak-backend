package no.nav.aap.postmottak.api.faktagrunnlag.tema

data class AvklarTemaVurderingDto(
    val skalTilAap: Boolean
)

data class AvklarTemaGrunnlagDto(
    val vurdering: AvklarTemaVurderingDto?,
    val dokumenter: List<String>,
    val journalpostMetadata: JournalpostMetadata
)

data class JournalpostMetadata(
    val brevkode: String?,
    val journalfoerendeEnhet: String?,
)
