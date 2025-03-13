package no.nav.aap.postmottak.journalpostogbehandling.journalpost

import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDate
import java.time.LocalDateTime

open class Journalpost(
    val journalpostId: JournalpostId,
    val person: Person,
    val journalførendeEnhet: String?,
    val tema: String,
    val behandlingstema: String?,
    val status: Journalstatus,
    val mottattDato: LocalDate,
    val mottattTid: LocalDateTime?,
    val dokumenter: List<Dokument> = emptyList(),
    val kanal: KanalFraKodeverk,
    val saksnummer: String?,
    val fagsystem: String?
) {

    val hoveddokumentbrevkode: String
        get() = hoveddokument.brevkode

    val hoveddokument: Dokument
        get() = dokumenter.minBy { it.dokumentInfoId.dokumentInfoId }

    fun erSøknad(): Boolean {
        return dokumenter.any {
            it.brevkode == Brevkoder.SØKNAD.kode
        }
    }
    
    fun behandlingstema(): String? = behandlingstema
    
    fun status(): Journalstatus = status

    fun dokumenter(): List<Dokument> = dokumenter

    fun mottattDato() = mottattDato

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
    
    fun erDigitalKlage(): Boolean {
        return dokumenter.any {
            it.brevkode == Brevkoder.KLAGE.kode
        } && erDigital()
    }

    fun erDigitaltMeldekort(): Boolean {
        return dokumenter.any {
            it.brevkode == Brevkoder.MELDEKORT.kode
        } && erDigital()
    }

    fun erDigital(): Boolean = finnOriginal()?.varianter?.any { it.filtype == Filtype.JSON } ?: false

    fun erPapir(): Boolean =
        kanal in listOf(KanalFraKodeverk.SKAN_IM, KanalFraKodeverk.SKAN_PEN, KanalFraKodeverk.SKAN_NETS)

    fun erUgyldig(): Boolean =
        status in listOf(Journalstatus.AVBRUTT, Journalstatus.FEILREGISTRERT, Journalstatus.UTGAAR)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Journalpost

        if (journalpostId != other.journalpostId) return false
        if (person != other.person) return false
        if (journalførendeEnhet != other.journalførendeEnhet) return false
        if (tema != other.tema) return false
        if (behandlingstema != other.behandlingstema) return false
        if (status != other.status) return false
        if (mottattDato != other.mottattDato) return false
        if (dokumenter != other.dokumenter) return false
        if (kanal != other.kanal) return false
        if (saksnummer != other.saksnummer) return false
        if (fagsystem != other.fagsystem) return false

        return true
    }

    override fun hashCode(): Int {
        var result = journalpostId.hashCode()
        result = 31 * result + person.hashCode()
        result = 31 * result + (journalførendeEnhet?.hashCode() ?: 0)
        result = 31 * result + tema.hashCode()
        result = 31 * result + (behandlingstema?.hashCode() ?: 0)
        result = 31 * result + status.hashCode()
        result = 31 * result + mottattDato.hashCode()
        result = 31 * result + dokumenter.hashCode()
        result = 31 * result + kanal.hashCode()
        result = 31 * result + (saksnummer?.hashCode() ?: 0)
        result = 31 * result + (fagsystem?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Journalpost(journalpostId=$journalpostId, person=$person, journalførendeEnhet=$journalførendeEnhet, tema='$tema', behandlingstema=$behandlingstema, status=$status, mottattDato=$mottattDato, dokumenter=$dokumenter, kanal=$kanal, saksnummer=$saksnummer, fagsystem=$fagsystem)"
    }
}

open class Dokument(
    val dokumentInfoId: DokumentInfoId,
    val brevkode: String,
    val varianter: List<Variant>
) {
    fun finnFiltype(variantformat: Variantformat): Filtype? =
        varianter.find { it.variantformat == variantformat }?.filtype

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Dokument

        if (dokumentInfoId != other.dokumentInfoId) return false
        if (brevkode != other.brevkode) return false
        if (varianter != other.varianter) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dokumentInfoId.hashCode()
        result = 31 * result + brevkode.hashCode()
        result = 31 * result + varianter.hashCode()
        return result
    }

    override fun toString(): String {
        return "Dokument(dokumentInfoId=$dokumentInfoId, brevkode='$brevkode', varianter=$varianter)"
    }
}

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