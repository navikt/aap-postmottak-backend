package no.nav.aap.postmottak.fordeler.regler

import no.nav.aap.postmottak.sakogbehandling.journalpost.Person
import no.nav.aap.verdityper.Brevkoder

data class RegelInput(
    val journalpostId: Long,
    val person: Person,
    val brevkode: String
)