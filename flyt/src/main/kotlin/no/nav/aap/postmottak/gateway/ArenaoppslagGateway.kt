package no.nav.aap.postmottak.gateway

import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkResponse
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import java.time.LocalDate

interface ArenaoppslagGateway : Gateway {
    suspend fun harHistorikk(person: Person): Boolean

    suspend fun harSignifikantHistorikk(person: Person, mottattDato: LocalDate): SignifikantHistorikkResponse

    suspend fun maksdatoForSaker(ident: Ident): List<SakMedSisteVedtakOgMaksdato>

    suspend fun sisteUtbetalingsdatoForPerson(ident: Ident): LocalDate?
}
