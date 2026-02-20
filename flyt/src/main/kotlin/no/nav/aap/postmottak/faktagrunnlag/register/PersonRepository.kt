package no.nav.aap.postmottak.faktagrunnlag.register

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import java.util.UUID

interface PersonRepository : Repository {
    fun finnEllerOpprett(identer: List<Ident>): Person
    fun oppdater(person: Person, identer: List<Ident>)
    fun hent(identifikator: UUID): Person
    fun hent(personId: Long): Person
    fun finn(ident: Ident): Person?
}