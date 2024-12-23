package no.nav.aap.fordeler.regler

import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person

data class RegelInput(
    val journalpostId: Long,
    val person: Person,
    val brevkode: String
)