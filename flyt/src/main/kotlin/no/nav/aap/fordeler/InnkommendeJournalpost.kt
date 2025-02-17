package no.nav.aap.fordeler

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

enum class InnkommendeJournalpostStatus{
    EVALUERT,
    VIDERESENDT
}

data class InnkommendeJournalpost(
    val journalpostId: JournalpostId,
    val brevkode: String?,
    val behandlingstema: String?,
    val status: InnkommendeJournalpostStatus,
    val regelresultat: Regelresultat,
)
