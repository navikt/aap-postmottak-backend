package no.nav.aap.fordeler.regler

import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import java.time.LocalDate

data class RegelInput(
    val journalpostId: Long,
    val person: Person,
    val brevkode: String,
    val mottattDato: LocalDate
)