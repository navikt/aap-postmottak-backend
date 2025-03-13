package no.nav.aap.postmottak.journalpostogbehandling.journalpost

import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDate
import java.time.LocalDateTime


class JournalpostMedDokumentTitler(
    journalpostId: JournalpostId,
    person: Person,
    journalførendeEnhet: String?,
    tema: String,
    behandlingstema: String?,
    status: Journalstatus,
    mottattDato: LocalDate,
    mottattTid: LocalDateTime?,
    dokumenter: List<DokumentMedTittel> = emptyList(),
    kanal: KanalFraKodeverk,
    saksnummer: String?,
    fagsystem: String?,
) : Journalpost(
    journalpostId,
    person,
    journalførendeEnhet,
    tema,
    behandlingstema,
    status,
    mottattDato,
    mottattTid,
    dokumenter,
    kanal,
    saksnummer,
    fagsystem
) {

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
) : Dokument(dokumentInfoId, brevkode, varianter)

