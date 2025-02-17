package no.nav.aap.postmottak.gateway

import no.nav.aap.api.intern.SakStatus
import no.nav.aap.lookup.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person

interface AapInternApiGateway: Gateway {
    fun hentAapSakerForPerson(person: Person): List<SakStatus>
}
