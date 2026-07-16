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

    suspend fun sisteVedtakMedMaksdato(ident: Ident): SakMedSisteVedtakOgMaksdato?

    suspend fun sisteUtbetalingsdatoForPerson(ident: Ident): LocalDate?

    /**
     * Henter data om søkers siste arenasak med AAP-vedtak, til bruk i manuell vurdering av fordeling.
     *
     * NB: Backend-API-et i Arena er ikke implementert enda – dette er foreløpig en dummy.
     */
    suspend fun hentArenasakForManuellVurdering(ident: Ident): ArenasakForManuellVurdering?
}

/**
 * Data som vises til saksbehandler ved manuell vurdering av fordeling (Kelvin/Arena).
 * Alle felter er nullable inntil Arena-API-et er på plass.
 */
data class ArenasakForManuellVurdering(
    val saksnummer: String?,
    val aktiv: Boolean?,
    val under52: Boolean?,
    val gjenstaendeOrdinaerPeriodeDager: Int?,
    val gjenstaendeUnntaksperiodeDager: Int?,
    val sisteAapVedtak: SisteAapVedtak?,
    val sisteUtbetaling: LocalDate?,
    val navKontoretsInnstillingUrl: String?,
)

data class SisteAapVedtak(
    val paragraf: String?,
    val beskrivelse: String?,
    val fom: LocalDate?,
    val tom: LocalDate?,
)

