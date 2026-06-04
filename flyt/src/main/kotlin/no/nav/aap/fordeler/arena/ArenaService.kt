package no.nav.aap.fordeler.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato
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
        val sisteSak = hentSisteSakMedEffektivMaksdato(søker)
        val maksdatoNærmerSeg = maksdatoNærmerSeg(sisteSak, mottattDato)

        // Dersom 11-12 allerede er innvilget for et kommende nytt år skal den ikke til manuell fordeling.
        // Den situasjonen gjenspeiles i maxdatoAap, og maxdatoAap vil da være forbi `terskeldato`.
        // Derfor er 11-12 situasjonen også håndtert, selv om det ikke er en eksplisitt sjekk for den.
        // TODO: når de blir tilgjengelige i Arena: sjekk vilkår for 11-12 (tre stk) direkte
        val tilManuellFordeling =
            sisteSak != null && maksdatoNærmerSeg && !sisteSak.ferdigAvklart && !sisteSak.utredesForUfor

        logger.info("Brukerens søknad fra $mottattDato er 'kant-i-kant': $tilManuellFordeling, sak: $sisteSak")

        return tilManuellFordeling
    }

    private fun maksdatoNærmerSeg(
        sisteSak: SakMedSisteVedtakOgMaksdato?, mottattDato: LocalDate
    ): Boolean {
        return sisteSak?.sisteVedtak?.maxdatoAap?.let { maxdato ->
            val maxdatoErIkkePassert = maxdato.isAfter(mottattDato)
            val terskeldato = mottattDato.plusWeeks(17L) // TODO revurdere etter vi har samlet statistikk

            // Sjekk om søknaden som har en definert maksdato er "kant-i-kant" med forrige søknad:
            (maxdatoErIkkePassert && maxdato <= terskeldato)
        } ?: false
    }

    private suspend fun hentSisteSakMedEffektivMaksdato(søker: Person): SakMedSisteVedtakOgMaksdato? {
        val maksdatoene = arena.maksdatoForSaker(søker.aktivIdent())

        val sisteSak = maksdatoene.filter { it.sisteVedtak.maxdatoAap != null }
            .maxByOrNull { it.sisteVedtak.maxdatoAap!! } // vet at de er ikke-null nå

        return when (sisteSak) {
            null -> null
            else if (sisteSak.sakAvsluttet != null
                    // fra_dato verdier før til_dato brukes i Arena for å markere vedtak som ugyldiggjorte
                    || sisteSak.sisteVedtak.fra?.isAfter(sisteSak.sisteVedtak.maxdatoAap) == true
                    || !sisteSak.lopendeVedtak) -> sisteSak.medUdefinertMaxsdato()

            else -> {
                sisteSak // har en gyldig maxdato
            }
        }
    }

    suspend fun maksimaltUtvidetKvoteSnartOppbrukt(søker: Person, mottattDato: LocalDate): Boolean {
        val sisteSak = hentSisteSakMedEffektivMaksdato(søker)

        // Gitt at det både er sendt søknad, og det er tidligere innvilget to år med utvidet kvote
        val behandlesSomNySøknad = when {
            sisteSak == null -> false
            sisteSak.utredesForUfor -> false
            sisteSak.ferdigAvklart -> false
            else -> {

                sisteSak.har_11_12_forlengelse // er tidligere forlenget
                        && sakenHarBegyntPåFemteÅret(mottattDato, sisteSak)
                        && maksdatoNærmerSeg(sisteSak, mottattDato)
            }
        }

        logger.info("Brukerens søknad fra $mottattDato kan behandles som en ny søknad: $behandlesSomNySøknad, sak: $sisteSak")

        return behandlesSomNySøknad
    }

    private fun sakenHarBegyntPåFemteÅret(
        mottattDato: LocalDate,
        sisteSak: SakMedSisteVedtakOgMaksdato
    ): Boolean {
        val fireOgEtHalvtÅrSiden = mottattDato.minusMonths(4 * 12 + 6) // 4.5 år

        return fireOgEtHalvtÅrSiden.isAfter(sisteSak.sakRegistrert)
    }

}