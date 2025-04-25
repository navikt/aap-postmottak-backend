package no.nav.aap.postmottak.gateway

import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person

interface AapInternApiGateway: Gateway {
    fun harAapSakIArena(person: Person): PersonEksistererIAAPArena
}
