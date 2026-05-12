package no.nav.aap.fordeler.arena

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost

private const val TERSKEL_VERDI_UKER = 13L

@Suppress("MagicNumber")
class ArenaService(gatewayProvider: GatewayProvider) {
    private val arena = gatewayProvider.provide(ArenaoppslagGateway::class)

    suspend fun erKantIKantSøknad(journalpost: Journalpost): Boolean {
        val søker = journalpost.person
        val mottattDato = journalpost.mottattDato
        val sisteMaxDato = arena.maksdatoForSaker(søker.aktivIdent())
            .filter { it.lopende }
            .mapNotNull { it.sisteVedtak.maxUnntakTil }
            .maxOrNull()

        val nærmerSegUtløp = sisteMaxDato != null && (sisteMaxDato <= mottattDato.plusWeeks(TERSKEL_VERDI_UKER))

        return nærmerSegUtløp
    }

    suspend fun harUtbetalingSiste52Uker(journalpost: Journalpost): Boolean {
        val søker = journalpost.person
        val søknadsdato = journalpost.mottattDato

        val sisteUtbetalingsDato = arena.sisteUtbetalingsdatoForPerson(søker.aktivIdent())
        if (sisteUtbetalingsDato == null) {
            return false
        } else {
            val tentativtVirkningstidspunkt = søknadsdato

            val ettÅrEtterSisteUtbetaling = sisteUtbetalingsDato.plusWeeks(52)

            return ettÅrEtterSisteUtbetaling >= tentativtVirkningstidspunkt
        }
    }

}