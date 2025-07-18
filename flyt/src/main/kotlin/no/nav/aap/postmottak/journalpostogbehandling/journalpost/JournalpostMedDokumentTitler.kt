package no.nav.aap.postmottak.journalpostogbehandling.journalpost

import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDate
import java.time.LocalDateTime


// TODO: Rydde opp i dette. Unødvendig med eget objekt for dokument med titler
class JournalpostMedDokumentTitler(
    journalpostId: JournalpostId,
    person: Person,
    journalførendeEnhet: String?,
    tema: String,
    behandlingstema: String?,
    tittel: String?,
    status: Journalstatus,
    mottattDato: LocalDate,
    mottattTid: LocalDateTime?,
    avsenderMottaker: AvsenderMottaker? = null,
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
    tittel,
    status,
    mottattDato,
    mottattTid,
    avsenderMottaker,
    dokumenter,
    kanal,
    saksnummer,
    fagsystem
) {

    fun getHoveddokumenttittel(): String = (hoveddokument as DokumentMedTittel).tittel ?: "Dokument uten tittel"

    fun getVedleggTitler(): List<String> = dokumenter.filter { it != hoveddokument }
        .map { it as DokumentMedTittel }
        .map { it.tittel ?: "Dokument uten tittel" }

}

class DokumentMedTittel(
    dokumentInfoId: DokumentInfoId,
    brevkode: String,
    tittel: String,
    varianter: List<Variant>
) : Dokument(dokumentInfoId, brevkode, tittel, varianter)

