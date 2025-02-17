package no.nav.aap.fordeler

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person

interface RegelRepository: Repository {
    fun hentRegelresultat(journalpostId: Long): Regelresultat
    fun hentPersonerMedJournalpostVideresendtTilKelvin(): List<Person>
    fun lagre(journalpostId: Long, regelresultat: Regelresultat)
}