package no.nav.aap.fordeler.arena

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost

@Suppress("MagicNumber")
class ArenaService(gatewayProvider: GatewayProvider) {
    private val arena = gatewayProvider.provide(ArenaoppslagGateway::class)

    suspend fun skalManueltFordeles(journalpost: Journalpost): Boolean {
        val søker = journalpost.person
        val mottattDato = journalpost.mottattDato
        val sisteMaxdato = arena.maksdatoForSaker(søker.aktivIdent())
            .filter { it.lopendeVedtak } // ikke Stans etc
            .filter { !it.utredesForUfor } // ikke 11-18 innvilget så langt
            .filter { !it.ferdigAvklart } // ikke 11-17 innvilget så langt
            .mapNotNull { it.sisteVedtak.maxdatoAap } // en maxdato finnes i Arena
            .maxOrNull()

        val terskeldato = mottattDato.plusWeeks(13L)

        return sisteMaxdato != null && (sisteMaxdato <= terskeldato)
    }

}