package no.nav.aap.postmottak.fordeler

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

enum class InnkommendeJournalpostStatus{
    EVALUERT,
    VIDERESENDT
}

data class InnkommendeJournalpost(
    val journalpostId: JournalpostId,
    val status: InnkommendeJournalpostStatus,
    val regelresultat: Regelresultat,
)
