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
     * Kaster [PersonIkkeFunnetIArenaException] dersom personen ikke finnes i Arena, og
     * [IngenAapSakIArenaException] dersom personen finnes men ikke har noen AAP-sak.
     */
    suspend fun hentArenasakForManuellVurdering(ident: Ident): ArenasakForManuellVurdering
}

/**
 * Data som vises til saksbehandler ved manuell vurdering av fordeling (Kelvin/Arena).
 * Speiler Arenas `VurderingsgrunnlagResponse`.
 */
data class ArenasakForManuellVurdering(
    val saksnummer: String?,
    val erAktiv: Boolean,
    val under52Uker: Boolean?,
    val gjenståendeOrdinæreDager: Int?,
    // Samlet gjenstående unntaksperiode §11-12 (andre og tredje ledd slås sammen).
    val gjenståendeUnntaksDager: Int?,
    val sisteVedtak: SisteVedtak?,
    val sisteUtbetaling: LocalDate?,
)

/**
 * Siste AAP-vedtak i arenasaken. Maksdatoene er ikke nødvendige når saken har gjenstående
 * periode (uttrykt via [ArenasakForManuellVurdering.gjenståendeOrdinæreDager]/`gjenståendeUnntaksDager`),
 * og er derfor nullable her.
 */
data class SisteVedtak(
    val vedtakId: Int,
    val aktfaseKode: String?,
    val vedtaktypeKode: String?,
    val fra: LocalDate?,
    val til: LocalDate?,
    val maxdatoOrdinaer: LocalDate?,
    val maxdatoUnntak: LocalDate?,
    val maxdatoAap: LocalDate?,
)

/** Kastes når personen ikke ble funnet i Arena (HTTP 404). */
class PersonIkkeFunnetIArenaException(melding: String) : RuntimeException(melding)

/** Kastes når personen finnes i Arena, men ikke har noen AAP-sak (HTTP 404). */
class IngenAapSakIArenaException(melding: String) : RuntimeException(melding)

