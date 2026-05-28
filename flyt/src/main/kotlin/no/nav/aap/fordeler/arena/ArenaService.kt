package no.nav.aap.fordeler.arena

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import org.slf4j.LoggerFactory
import java.time.LocalDate

@Suppress("MagicNumber")
class ArenaService(gatewayProvider: GatewayProvider) {
    private val arena = gatewayProvider.provide(ArenaoppslagGateway::class)
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun skalManueltFordeles(søker: Person, mottattDato: LocalDate): Boolean {
        val maksdatoene = arena.maksdatoForSaker(søker.aktivIdent())
        val sisteSak = maksdatoene
            .filter { it.sisteVedtak.maxdatoAap != null }
            .maxByOrNull { it.sisteVedtak.maxdatoAap!! } // vet at de er ikke-null nå

        val tilManuellFordeling = when (sisteSak) {
            null -> false
            else if (
                    sisteSak.sakAvsluttet != null
                    || sisteSak.ferdigAvklart
                    || sisteSak.utredesForUfor
                    || !sisteSak.lopendeVedtak
                    // Dersom 11-12 allerede er innvilget for et nytt år skal den ikke til manuell fordeling.
                    // Den situasjonen gjenspeiles i maxdatoAap, og maxdatoAap vil da være forbi `terskeldato`.
                    // Derfor er 11-12 situasjonen også håndtert, selv om det ikke er en eksplisitt sjekk for den.
                    ) -> false

            else -> {
                val maxdato = sisteSak.sisteVedtak.maxdatoAap!! // vet at det er ikke-null nå
                val maxdatoErIkkePassert = maxdato.isAfter(mottattDato)
                val terskeldato = mottattDato.plusWeeks(17L) // TODO revurdere etter vi har samlet statistikk

                // Sjekk om søknaden er "kant-i-kant" med forrige søknad:
                maxdatoErIkkePassert && maxdato <= terskeldato
            }
        }
        logger.info("Brukerens søknad fra $mottattDato er 'kant-i-kant': $tilManuellFordeling, sak: $sisteSak")

        return tilManuellFordeling
    }

}