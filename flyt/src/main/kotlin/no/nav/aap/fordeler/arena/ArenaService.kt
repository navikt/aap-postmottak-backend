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

        // Dersom kriteriene ovenfor er oppfylt og søknaden kommer innenfor en gitt tid før maksdato,
        // går den til manuell fordeling.
        // Dersom 11-12 allerede er innvilget for et nytt år skal den ikke til manuell fordeling.
        // Den situasjonen gjenspeiles i maxdatoAap, og maxdatoAap vil da være forbi `terskeldato`.

        return sisteMaxdato != null && (sisteMaxdato <= terskeldato)
    }

}