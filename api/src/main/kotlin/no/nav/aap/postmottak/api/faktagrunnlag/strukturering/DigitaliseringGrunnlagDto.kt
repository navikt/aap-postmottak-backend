package no.nav.aap.postmottak.api.faktagrunnlag.strukturering

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.postmottak.gateway.Klagebehandling
import java.time.LocalDate

data class DigitaliseringvurderingDto(
    val kategori: InnsendingType,
    val strukturertDokumentJson: String?,
    val søknadsdato: LocalDate?
)

data class JournalpostMetadata(
    val kategori: InnsendingType?,
    val brevkode: String?,
    /**
     * Ikke alle digitale dokumenter har strukturert data
     */
    val harStrukturertData: Boolean,
    val erPapir: Boolean
)

data class DigitaliseringGrunnlagDto(
    val klagebehandlinger: List<Klagebehandling>,
    val erPapir: Boolean,
    val journalpostMetadata: JournalpostMetadata,
    val vurdering: DigitaliseringvurderingDto?
)
