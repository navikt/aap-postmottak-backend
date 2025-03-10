package no.nav.aap.fordeler

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

enum class InnkommendeJournalpostStatus{
    EVALUERT,
    VIDERSENDT_TIL_KELVIN,
    VIDERESENDT_TIL_ARENA,
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
    val årsakTilStatus: ÅrsakTilStatus? = null
) {
    init {
        if (status == InnkommendeJournalpostStatus.EVALUERT && regelresultat == null) {
            throw IllegalArgumentException("Regelresultat må være satt når status er EVALUERT")
        }
    }
}
