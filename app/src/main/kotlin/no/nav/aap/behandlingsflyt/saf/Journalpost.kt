package no.nav.aap.behandlingsflyt.saf

import java.time.LocalDate

const val SKJEMANUMMER_SØKNAD = "NAV 11-13.05"

sealed class Journalpost(
    open val journalpostId: Long,
    open val journalførendeEnhet: String?,
    private val status: JournalpostStatus,
    private val mottattDato: LocalDate,
    private val dokumenter: List<Dokument> = emptyList()
) {
    fun harFortsattTilstandMottatt(): Boolean {
        return status == JournalpostStatus.MOTTATT
    }

    fun erSøknad(): Boolean {
        return dokumenter.any {
            it.brevkode == SKJEMANUMMER_SØKNAD
        }
    }

    fun mottattDato() = mottattDato

    fun finnOriginal(): Dokument? = dokumenter.find {
        it.variantFormat == Variantformat.ORIGINAL
    }

    fun erDigital(): Boolean = finnOriginal()?.filtype == Filtype.JSON

    data class UtenIdent(
        override val journalpostId: Long,
        override val journalførendeEnhet: String?,
        private val status: JournalpostStatus,
        private val mottattDato: LocalDate,
        private val dokumenter: List<Dokument>
    ) : Journalpost(journalpostId, journalførendeEnhet, status, mottattDato, dokumenter)

    data class MedIdent(
        val personident: Ident,
        override val journalpostId: Long,
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
}

enum class JournalpostStatus {
    MOTTATT,
    JOURNALFØRT,
    UKJENT
}

data class Dokument(
    val dokumentInfoId: String,
    val variantFormat: Variantformat,
    val filtype: Filtype,
    val brevkode: String?
)

enum class Filtype {
    PDF, JPEG, PNG, TIFF, XLSX, JSON, XML, AXML, DXML, RTF
}

enum class Variantformat {
    ARKIV, FULLVERSJON, PRODUKSJON, PRODUKSJON_DLF, SLADDET, ORIGINAL
}