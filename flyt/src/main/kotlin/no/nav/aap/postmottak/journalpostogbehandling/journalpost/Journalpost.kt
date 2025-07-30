package no.nav.aap.postmottak.journalpostogbehandling.journalpost

import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDate
import java.time.LocalDateTime

data class Journalpost(
    val journalpostId: JournalpostId,
    val person: Person,
    val journalførendeEnhet: String?,
    val tema: String,
    val behandlingstema: String?,
    val tittel: String?,
    val status: Journalstatus,
    val mottattDato: LocalDate,
    val mottattTid: LocalDateTime?,
    val avsenderMottaker: AvsenderMottaker?,
    val dokumenter: List<Dokument> = emptyList(),
    val kanal: KanalFraKodeverk,
    val saksnummer: String?,
    val fagsystem: String?
) {

    val hoveddokumentbrevkode: String
        get() = hoveddokument.brevkode

    val hoveddokument: Dokument
        get() = dokumenter.first()

    fun erSøknad(): Boolean {
        return dokumenter.any {
            it.brevkode == Brevkoder.SØKNAD.kode
        }
    }

    // TODO: Bør ikke denne sjekke hoveddokumentet - ikke bare returere første treff?
    fun finnOriginal(): Dokument? = dokumenter.find { dokument ->
        dokument.varianter.any { variant -> variant.variantformat == Variantformat.ORIGINAL }
    }

    fun finnArkivVarianter(): List<Dokument> = dokumenter.filter { dokument ->
        dokument.varianter.any { variant -> variant.variantformat == Variantformat.ARKIV }
    }

    fun erDigitalSøknad(): Boolean {
        return erSøknad() && erDigital()
    }

    fun erDigitalLegeerklæring(): Boolean {
        return dokumenter.any {
            it.brevkode == Brevkoder.LEGEERKLÆRING.kode
        } && erDigital()
    }

    fun erDigitaltMeldekort(): Boolean {
        val meldekortkoder = listOf(Brevkoder.MELDEKORT.kode, Brevkoder.MELDEKORT_KORRIGERING.kode)
        return dokumenter.any { it.brevkode in meldekortkoder } && erDigital()
    }

    fun erDigital(): Boolean = finnOriginal()?.varianter?.any { it.filtype == Filtype.JSON } ?: false

    fun erPapir(): Boolean =
        kanal in listOf(KanalFraKodeverk.SKAN_IM, KanalFraKodeverk.SKAN_PEN, KanalFraKodeverk.SKAN_NETS)

    fun erUgyldig(): Boolean =
        status in listOf(Journalstatus.AVBRUTT, Journalstatus.FEILREGISTRERT, Journalstatus.UTGAAR)

    fun getHoveddokumenttittel(): String = hoveddokument.tittel ?: "Dokument uten tittel"

    fun getVedleggTitler(): List<String> = dokumenter
        .filter { it.dokumentInfoId != hoveddokument.dokumentInfoId }
        .map { it.tittel ?: "Dokument uten tittel" }
}

data class Dokument(
    val dokumentInfoId: DokumentInfoId,
    val brevkode: String,
    val tittel: String? = null,
    val varianter: List<Variant>,
)

data class Variant(
    val filtype: Filtype,
    val variantformat: Variantformat,
)

enum class Filtype {
    PDF, JPEG, PNG, TIFF, XLSX, JSON, XML, AXML, DXML, RTF
}

enum class Variantformat {
    ARKIV, FULLVERSJON, PRODUKSJON, PRODUKSJON_DLF, SLADDET, ORIGINAL
}

data class AvsenderMottaker(
    val id: String?,
    val idType: String?,
    val navn: String? = null,
)
