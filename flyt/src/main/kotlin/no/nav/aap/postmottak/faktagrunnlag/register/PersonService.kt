package no.nav.aap.postmottak.faktagrunnlag.register

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.faktagrunnlag.register.PersonRepository
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person

class PersonService(private val personRepository: PersonRepository, private val persondataGateway: PersondataGateway) {
    companion object {
        fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): PersonService {
            return PersonService(
                repositoryProvider.provide(),
                gatewayProvider.provide()
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