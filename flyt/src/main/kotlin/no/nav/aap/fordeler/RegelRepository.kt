package no.nav.aap.fordeler

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

interface RegelRepository: Repository {
    fun hentRegelresultat(journalpostId: JournalpostId): Regelresultat?
    fun hentRegelresultat(innkommendeJournalpostId: Long): Regelresultat?
    fun hentPersonerMedJournalpostVideresendtTilKelvin(): List<Person>
    fun lagre(innkommendeJournalpostId: Long, regelresultat: Regelresultat)
}