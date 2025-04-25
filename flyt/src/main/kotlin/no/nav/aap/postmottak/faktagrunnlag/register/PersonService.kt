package no.nav.aap.postmottak.faktagrunnlag.register

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.journalpostogbehandling.db.PersonRepository
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person

class PersonService(private val personRepository: PersonRepository, private val persondataGateway: PersondataGateway) {
    companion object {
        fun konstruer(connection: DBConnection): PersonService {
            return PersonService(
                RepositoryProvider(connection).provide(PersonRepository::class),
                GatewayProvider.provide(PersondataGateway::class)
            )
        }
    }

    fun finnOgOppdaterPerson(ident: String): Person {
        val identliste = persondataGateway.hentAlleIdenterForPerson(ident)

        if (identliste.isEmpty()) {
            throw IllegalStateException("Fikk ingen treff på ident i PDL")
        }
        val person = personRepository.finnEllerOpprett(identliste)

        return person
    }
    
    

}