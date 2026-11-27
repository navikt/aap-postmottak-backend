package no.nav.aap.postmottak.test.inmemoryrepo

import no.nav.aap.postmottak.faktagrunnlag.register.PersonRepository
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object InMemoryPersonRepository : PersonRepository {
    private val persons = ConcurrentHashMap<Long, Person>()

    fun lagre(person: Person): Person {
        persons[person.id] = person
        return person
    }

    override fun finnEllerOpprett(identer: List<Ident>): Person {
        val eksisterende = persons.values.filter { person -> identer.any(person::er) }.distinctBy { it.identifikator }
        check(eksisterende.size <= 1) { "Identene tilhører flere personer" }
        val person = eksisterende.singleOrNull()
            ?: Person(InMemoryIdSequence.nestePersonId(), UUID.randomUUID(), identer)
        val oppdatert = Person(
            id = person.id,
            identifikator = person.identifikator,
            identer = (person.identer() + identer).distinct(),
        )
        return lagre(oppdatert)
    }

    override fun oppdater(person: Person, identer: List<Ident>) {
        lagre(
            Person(
                id = person.id,
                identifikator = person.identifikator,
                identer = (person.identer() + identer).distinct(),
            )
        )
    }

    override fun hent(identifikator: UUID) =
        persons.values.single { it.identifikator == identifikator }

    override fun hent(personId: Long) =
        checkNotNull(persons[personId]) { "Fant ikke person $personId" }

    override fun finn(ident: Ident) = persons.values.singleOrNull { it.er(ident) }
}
