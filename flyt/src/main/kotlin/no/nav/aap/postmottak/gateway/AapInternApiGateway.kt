package no.nav.aap.postmottak.gateway

import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.api.intern.SignifikanteSakerResponse
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import java.time.LocalDate

interface AapInternApiGateway : Gateway {
    fun harAapSakIArena(person: Person): PersonEksistererIAAPArena

    fun harSignifikantHistorikkIAAPArena(person: Person, mottattDato: LocalDate): SignifikanteSakerResponse
}
