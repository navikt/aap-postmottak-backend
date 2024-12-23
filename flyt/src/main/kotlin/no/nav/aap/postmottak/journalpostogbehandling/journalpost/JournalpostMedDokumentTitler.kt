package no.nav.aap.postmottak.journalpostogbehandling.journalpost

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import java.time.LocalDate


class JournalpostMedDokumentTitler(
    journalpostId: JournalpostId,
    person: Person,
    journalførendeEnhet: String?,
    tema: String,
    status: JournalpostStatus,
    mottattDato: LocalDate,
    dokumenter: List<DokumentMedTittel> = emptyList(),
    kanal: KanalFraKodeverk
): Journalpost(journalpostId, person, journalførendeEnhet, tema, status, mottattDato, dokumenter, kanal) {

    fun getHoveddokumenttittel() = (hoveddokument as DokumentMedTittel).tittel

    fun getVedleggTitler() = dokumenter.filter { it != hoveddokument }
        .map { it as DokumentMedTittel }
        .map { it.tittel }

}

class DokumentMedTittel(
    dokumentInfoId: DokumentInfoId,
    variantFormat: Variantformat,
    filtype: Filtype,
    brevkode: String,
    val tittel: String
): Dokument(dokumentInfoId, variantFormat, filtype, brevkode)

