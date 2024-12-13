package no.nav.aap.postmottak.sakogbehandling.journalpost

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.verdityper.Brevkoder
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.KanalFraKodeverk
import java.time.LocalDate

open class Journalpost(
    val journalpostId: JournalpostId,
    val person: Person,
    val journalførendeEnhet: String?,
    val tema: String,
    val status: JournalpostStatus,
    val mottattDato: LocalDate,
    val dokumenter: List<Dokument> = emptyList(),
    val kanal: KanalFraKodeverk
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
    
    fun status(): JournalpostStatus = status
    
    fun dokumenter(): List<Dokument> = dokumenter
    
    fun mottattDato() = mottattDato

    fun finnOriginal(): Dokument? = dokumenter.find {
        it.variantFormat == Variantformat.ORIGINAL
    }

    fun finnArkivVarianter(): List<Dokument> = dokumenter.filter {
         it.variantFormat == Variantformat.ARKIV
    }

    fun erDigitalSøknad(): Boolean {
        return erSøknad() && erDigital()
    }
    
    fun erDigitalLegeerklæring(): Boolean {
        return dokumenter.any {
            it.brevkode == Brevkoder.LEGEERKLÆRING.kode
        } && erDigital()
    }

    fun erDigital(): Boolean = finnOriginal()?.filtype == Filtype.JSON
}

enum class JournalpostStatus {
    MOTTATT,
    JOURNALFØRT,
    UKJENT
}

open class Dokument(
    val dokumentInfoId: DokumentInfoId,
    val variantFormat: Variantformat,
    val filtype: Filtype,
    val brevkode: String,
)

enum class Filtype {
    PDF, JPEG, PNG, TIFF, XLSX, JSON, XML, AXML, DXML, RTF
}

enum class Variantformat {
    ARKIV, FULLVERSJON, PRODUKSJON, PRODUKSJON_DLF, SLADDET, ORIGINAL
}