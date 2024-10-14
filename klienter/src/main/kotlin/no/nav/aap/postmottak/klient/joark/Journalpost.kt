package no.nav.aap.postmottak.klient.joark

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDate

const val SKJEMANUMMER_SØKNAD = "NAV 11-13.05"

// TODO: Bør skille SAF-respons fra domenemodell
sealed class Journalpost(
    open val journalpostId: JournalpostId,
    open val journalførendeEnhet: String?,
    private val status: JournalpostStatus,
    private val mottattDato: LocalDate,
    private val dokumenter: List<Dokument> = emptyList()
) {

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

    data class UtenIdent(
        override val journalpostId: JournalpostId,
        override val journalførendeEnhet: String?,
        private val status: JournalpostStatus,
        private val mottattDato: LocalDate,
        private val dokumenter: List<Dokument>
    ) : Journalpost(journalpostId, journalførendeEnhet, status, mottattDato, dokumenter)

    data class MedIdent(
        val personident: Ident,
        override val journalpostId: JournalpostId,
        override val journalførendeEnhet: String?,
        private val status: JournalpostStatus,
        private val mottattDato: LocalDate,
        private val dokumenter: List<Dokument>
    ) : Journalpost(journalpostId, journalførendeEnhet, status, mottattDato, dokumenter)
}

sealed class Ident(
    open val id: String
) {
    class Personident(id: String) : Ident(id)
    class Aktørid(id: String) : Ident(id)
    
    override fun equals (other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Ident

        if (id != other.id) return false

        return true
    }
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