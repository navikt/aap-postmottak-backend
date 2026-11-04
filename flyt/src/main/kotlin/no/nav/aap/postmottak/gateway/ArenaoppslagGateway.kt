package no.nav.aap.postmottak.gateway

import no.nav.aap.arenaoppslag.kontrakt.apiv1.HistorikkStatusDetaljResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkResponse
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import java.time.LocalDate

interface ArenaoppslagGateway : Gateway {
    suspend fun harHistorikk(person: Person): Boolean

    suspend fun harSignifikantHistorikk(person: Person, mottattDato: LocalDate): SignifikantHistorikkResponse

    suspend fun historikkStatusDetalj(person: Person, mottattDato: LocalDate): HistorikkStatusDetaljResponse
}

