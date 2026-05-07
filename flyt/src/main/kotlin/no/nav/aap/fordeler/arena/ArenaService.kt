package no.nav.aap.fordeler.arena

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import java.time.LocalDate

private const val TERSKEL_VERDI_UKER = 13L

class ArenaService(gatewayProvider: GatewayProvider) {
    private val arena = gatewayProvider.provide(ArenaoppslagGateway::class)

    suspend fun erKantIKantSøknad(journalpost: Journalpost): Boolean {
        val søker = journalpost.person
        val sakerMedSignifikantHistorikk = arena.hentSakerMedSignifikantHistorikk(
            søker,
            journalpost.mottattDato
        )
        // TODO endre API til å se på alle AAP-saker for personen som er AKTIVE?
        val sisteMaxDato = arena.maksdatoForSaker(sakerMedSignifikantHistorikk)
            .filter { it.lopende }
            .mapNotNull { it.sisteVedtak.maxUnntakTil }
            .maxOrNull()

        val nærmerSegUtløp = sisteMaxDato != null && (sisteMaxDato <= LocalDate.now().plusWeeks(TERSKEL_VERDI_UKER))

        return nærmerSegUtløp
    }

    suspend fun harUtbetalingSiste52Uker(journalpost: Journalpost): Boolean {
        val søker = journalpost.person
        val søknadsdato = journalpost.mottattDato
        val sakerMedSignifikantHistorikk = arena.hentSakerMedSignifikantHistorikk(
            søker,
            journalpost.mottattDato
        )

        // TODO oppdater API til å heller ta fodselsnummer
        val sisteUtbetalingsDato = arena.sisteUtbetalingsdatoForSaker(sakerMedSignifikantHistorikk)
            .mapNotNull { it.sisteAAPUtbetalingsdato }
            .maxOrNull()

        val tentativtVirkningstidspunkt = søknadsdato
        val søknadKommerInnenfor52UkerAvForrigeUtbetaling =
            sisteUtbetalingsDato != null && sisteUtbetalingsDato.plusWeeks(52) >= tentativtVirkningstidspunkt

        return søknadKommerInnenfor52UkerAvForrigeUtbetaling
    }

}