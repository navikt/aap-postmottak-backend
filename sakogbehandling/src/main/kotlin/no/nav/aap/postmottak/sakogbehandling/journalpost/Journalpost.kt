package no.nav.aap.postmottak.sakogbehandling.journalpost

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDate

const val SKJEMANUMMER_SØKNAD = "NAV 11-13.05"

// TODO: Bør skille SAF-respons fra domenemodell
data class Journalpost(
    val journalpostId: JournalpostId,
    val person: Person,
    val journalførendeEnhet: String?,
    val tema: String,
    val status: JournalpostStatus,
    val mottattDato: LocalDate,
    val dokumenter: List<Dokument> = emptyList()
) {

    val hoveddokumentbrevkode: String
        get() = dokumenter.minBy { it.dokumentInfoId.dokumentInfoId }.brevkode !!

    fun erSøknad(): Boolean {
        return dokumenter.any {
            it.brevkode == SKJEMANUMMER_SØKNAD
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

    fun kanBehandlesAutomatisk(): Boolean {
        return erSøknad() && erDigital()
    }

    fun erDigital(): Boolean = finnOriginal()?.filtype == Filtype.JSON
}

enum class JournalpostStatus {
    MOTTATT,
    JOURNALFØRT,
    UKJENT
}

data class Dokument(
    val dokumentInfoId: DokumentInfoId,
    val variantFormat: Variantformat,
    val filtype: Filtype,
    val brevkode: String?,
)

enum class Filtype {
    PDF, JPEG, PNG, TIFF, XLSX, JSON, XML, AXML, DXML, RTF
}

enum class Variantformat {
    ARKIV, FULLVERSJON, PRODUKSJON, PRODUKSJON_DLF, SLADDET, ORIGINAL
}