package no.nav.aap.fordeler

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

enum class InnkommendeJournalpostStatus{
    EVALUERT,
    // Ikke lenger i bruk, men beholdes for å kunne lese eksisterende rader i databasen
    @Deprecated("Ikke i bruk lenger, beholdes for historiske data")
    VIDERSENDT_TIL_KELVIN,
    @Deprecated("Ikke i bruk lenger, beholdes for historiske data")
    VIDERESENDT_TIL_ARENA,
    @Deprecated("Ikke i bruk lenger, beholdes for historiske data")
    GOSYS_JFR,
    GOSYS_FDR,
    IGNORERT,
}

enum class ÅrsakTilStatus{
    MANGLER_IDENT,
    ORGNR,
    ALLEREDE_JOURNALFØRT,
    UTGÅTT
}

data class InnkommendeJournalpost(
    val journalpostId: JournalpostId,
    val brevkode: String?,
    val behandlingstema: String?,
    val status: InnkommendeJournalpostStatus,
    val regelresultat: Regelresultat? = null,
    val årsakTilStatus: ÅrsakTilStatus? = null,
    val enhet: NavEnhet? = null,
    val brukerId: String? = null,
)
