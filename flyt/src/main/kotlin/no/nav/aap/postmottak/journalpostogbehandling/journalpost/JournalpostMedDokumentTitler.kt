package no.nav.aap.postmottak.journalpostogbehandling.journalpost

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import java.time.LocalDate


class JournalpostMedDokumentTitler(
    journalpostId: JournalpostId,
    person: Person,
    journalførendeEnhet: String?,
    tema: String,
    status: Journalstatus,
    mottattDato: LocalDate,
    dokumenter: List<DokumentMedTittel> = emptyList(),
    kanal: KanalFraKodeverk,
    saksnummer: Saksnummer? = null,
): Journalpost(journalpostId, person, journalførendeEnhet, tema, status, mottattDato, dokumenter, kanal, saksnummer) {

    fun getHoveddokumenttittel() = (hoveddokument as DokumentMedTittel).tittel

    fun getVedleggTitler() = dokumenter.filter { it != hoveddokument }
        .map { it as DokumentMedTittel }
        .map { it.tittel }

}

class DokumentMedTittel(
    dokumentInfoId: DokumentInfoId,
    brevkode: String,
    val tittel: String,
    varianter: List<Variant>
): Dokument(dokumentInfoId, brevkode, varianter)

