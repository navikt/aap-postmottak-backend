package no.nav.aap.postmottak.journalpostogbehandling.journalpost

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDate

open class Journalpost(
    val journalpostId: JournalpostId,
    val person: Person,
    val journalførendeEnhet: String?,
    val tema: String,
    val status: Journalstatus,
    val mottattDato: LocalDate,
    val dokumenter: List<Dokument> = emptyList(),
    val kanal: KanalFraKodeverk,
    val saksnummer: Saksnummer?,
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

    fun status(): Journalstatus = status

    fun dokumenter(): List<Dokument> = dokumenter

    fun mottattDato() = mottattDato

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

    fun erDigital(): Boolean = finnOriginal()?.varianter?.any { it.filtype == Filtype.JSON } ?: false

    fun erUgyldig(): Boolean =
        status in listOf(Journalstatus.AVBRUTT, Journalstatus.FEILREGISTRERT, Journalstatus.UTGAAR)
}

open class Dokument(
    val dokumentInfoId: DokumentInfoId,
    val brevkode: String,
    val varianter: List<Variant>
) {
    fun finnFiltype(variantformat: Variantformat): Filtype? = varianter.find { it.variantformat == variantformat }?.filtype
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