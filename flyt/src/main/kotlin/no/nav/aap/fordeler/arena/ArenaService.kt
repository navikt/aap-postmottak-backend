package no.nav.aap.fordeler.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkResponse
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import org.slf4j.LoggerFactory
import java.time.LocalDate

@Suppress("MagicNumber")
class ArenaService(gatewayProvider: GatewayProvider) {
    private val arena = gatewayProvider.provide(ArenaoppslagGateway::class)
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun skalManueltFordeles(søker: Person, mottattDato: LocalDate, signifikantHistorikk: SignifikantHistorikkResponse): Boolean {
        val sisteSak = hentSisteSakMedEffektivMaksdato(søker)
        return sisteSak?.let {
            val maksdatoNærmerSeg = maksdatoNærmerSeg(sisteSak, mottattDato)
            val flereSignifikanteSaker = harFlereSignifikanteSaker(signifikantHistorikk, sisteSak)
            val signifikanteVedtakerUtoverTypeAap = harSignifikanteVedtakUtoverTypeAap(signifikantHistorikk)

            // Dersom 11-12 allerede er innvilget for et kommende nytt år skal den ikke til manuell fordeling.
            // Den situasjonen gjenspeiles i maxdatoAap, og maxdatoAap vil da være forbi `terskeldato`.
            // Derfor er 11-12 situasjonen også håndtert, selv om det ikke er en eksplisitt sjekk for den.
            // TODO: når de blir tilgjengelige i Arena: sjekk vilkår for 11-12 (tre stk) direkte

            val tilManuellFordeling = !signifikanteVedtakerUtoverTypeAap && !flereSignifikanteSaker
                    && maksdatoNærmerSeg && !sisteSak.ferdigAvklart && !sisteSak.utredesForUfor

            logger.info("Brukerens søknad fra $mottattDato er 'kant-i-kant': $tilManuellFordeling, sak: $sisteSak")

            return tilManuellFordeling

        } ?: false
    }

    private fun harSignifikanteVedtakUtoverTypeAap(historikk: SignifikantHistorikkResponse): Boolean {
        val rettighetskoder = historikk.signifikanteVedtak.map { it.rettighetkode }.distinct()
        // hvis annet enn AAP
        return !rettighetskoder.contains("AAP") || rettighetskoder.size > 1
    }

    private fun harFlereSignifikanteSaker(
        historikk: SignifikantHistorikkResponse,
        sisteSak: SakMedSisteVedtakOgMaksdato
    ): Boolean {
        val flereSignifikanteSaker = historikk.saker().toMutableSet().apply {
            remove(sisteSak.sakId)
        }.isNotEmpty()
        return flereSignifikanteSaker
    }

    private fun maksdatoNærmerSeg(
        sisteSak: SakMedSisteVedtakOgMaksdato?, mottattDato: LocalDate
    ): Boolean {
        return sisteSak?.sisteVedtak?.maxdatoAap?.let { maxdato ->
            val maxdatoErIkkePassert = maxdato.isAfter(mottattDato)
            // TODO revurdere tidsperioden etter vi har samlet statistikk, kanskje bytt til 6mnd
            val terskeldato = mottattDato.plusWeeks(17L)

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

    suspend fun kanFordelesAutomatiskPga11_12_erMakset(
        søker: Person,
        mottattDato: LocalDate,
        signifikanteSaker: SignifikantHistorikkResponse
    ): Boolean {
        val sisteSak = hentSisteSakMedEffektivMaksdato(søker)

        // Gitt at det både er sendt søknad, og det er tidligere innvilget to år med utvidet kvote
        val behandlesSomNySøknad = when {
            sisteSak == null -> false
            sisteSak.utredesForUfor -> false
            sisteSak.ferdigAvklart -> false
            else -> {
                val flereSignifikanteSaker = harFlereSignifikanteSaker(signifikanteSaker, sisteSak)
                val signifikanteVedtakerUtoverTypeAap = harSignifikanteVedtakUtoverTypeAap(signifikanteSaker)

                sisteSak.har_11_12_forlengelse // er tidligere forlenget
                        && sakenHarBegyntPåAndreÅretMedUnntak(mottattDato, sisteSak) // på andre året
                        && maksdatoNærmerSeg(sisteSak, mottattDato) // og utløpet av ytelsen nærmer seg
                        && !signifikanteVedtakerUtoverTypeAap && !flereSignifikanteSaker
            }
        }

        logger.info("Brukerens søknad fra $mottattDato kan behandles som en ny søknad: $behandlesSomNySøknad, sak: $sisteSak")

        return behandlesSomNySøknad
    }

    private fun sakenHarBegyntPåAndreÅretMedUnntak(
        mottattDato: LocalDate,
        sisteSak: SakMedSisteVedtakOgMaksdato
    ): Boolean {
        val ettOgEtHalvtÅrSiden = mottattDato.minusMonths(18)
        return sisteSak.unntaksvilkaarGjelderFra?.isBefore(ettOgEtHalvtÅrSiden) ?: false
    }

}