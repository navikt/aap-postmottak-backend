package no.nav.aap.fordeler.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak
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

    suspend fun skalManueltFordeles(
        søker: Person, mottattDato: LocalDate, journalpostId: Long, signifikantHistorikk: SignifikantHistorikkResponse
    ): Boolean {
        val sisteSak = hentSisteSakMedEffektivMaksdato(søker)
        return sisteSak?.let {
            val maksdatoNærmerSeg = maksdatoNærmerSeg(sisteSak, mottattDato)
            val flereSignifikanteSaker = harFlereSignifikanteSaker(signifikantHistorikk.saker(), sisteSak)
            val signifikanteVedtakUtoverTypeAap = harSignifikanteVedtakUtoverTypeAap(signifikantHistorikk.signifikanteVedtak)

            // Dersom 11-12 allerede er innvilget for et kommende nytt år skal den ikke til manuell fordeling.
            // Den situasjonen gjenspeiles i maxdatoAap, og maxdatoAap vil da være forbi `terskeldato`.
            // Derfor er 11-12 situasjonen også håndtert, selv om det ikke er en eksplisitt sjekk for den.
            // TODO: når de blir tilgjengelige i Arena: sjekk vilkår for 11-12 (tre stk) direkte

            val tilManuellFordeling = !signifikanteVedtakUtoverTypeAap && !flereSignifikanteSaker && maksdatoNærmerSeg && !sisteSak.ferdigAvklart && !sisteSak.utredesForUfor
            val tilstand = tilstandSomString(signifikanteVedtakUtoverTypeAap, flereSignifikanteSaker, maksdatoNærmerSeg)

            logger.info("Journalpost $journalpostId er 'kant-i-kant': $tilManuellFordeling, sak: $sisteSak, tilstand: $tilstand")

            return tilManuellFordeling

        } ?: false
    }

    private fun tilstandSomString(
        signifikanteVedtakUtoverTypeAap: Boolean, flereSignifikanteSaker: Boolean, maksdatoNærmerSeg: Boolean
    ): String {
        val logmsg: StringBuilder = StringBuilder()
        logmsg.append("signifikanteVedtakUtoverTypeAap=$signifikanteVedtakUtoverTypeAap,")
        logmsg.append("flereSignifikanteSaker=$flereSignifikanteSaker,")
        logmsg.append("maksdatoNærmerSeg=$maksdatoNærmerSeg,")
        return logmsg.toString()
    }


    internal fun harSignifikanteVedtakUtoverTypeAap(signifikanteVedtak: List<ArenaVedtak>): Boolean {
        val rettighetskoder = signifikanteVedtak.map { it.rettighetkode }.toMutableSet()
        rettighetskoder.removeAll(listOf("AAP", "AA115")) // klager, anker og annet gjenstår

        return rettighetskoder.isNotEmpty()
    }

    internal fun harFlereSignifikanteSaker(
        saker: List<Int>, sisteSak: SakMedSisteVedtakOgMaksdato?
    ): Boolean {
        return if (sisteSak == null) {
            false
        } else {
            saker.toMutableSet().apply {
                remove(sisteSak.sakId)
            }.isNotEmpty()
        }
    }

    private fun maksdatoNærmerSeg(
        sisteSak: SakMedSisteVedtakOgMaksdato?, mottattDato: LocalDate
    ): Boolean {
        return sisteSak?.sisteVedtak?.maxdatoAap?.let { maxdato ->
            val maxdatoErIkkePassert = maxdato.isAfter(mottattDato)
            val terskeldato = mottattDato.plusWeeks(20L)

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
        søker: Person, mottattDato: LocalDate, journalpostId: Long, signifikanteSaker: SignifikantHistorikkResponse
    ): Boolean {
        val sisteSak = hentSisteSakMedEffektivMaksdato(søker)
        val maksdatoNærmerSeg = maksdatoNærmerSeg(sisteSak, mottattDato)
        val har11_12Forlengelse = sisteSak?.har_11_12_forlengelse == true
        val sakenHarBegyntPåAndreÅretMedUnntak = sakenHarBegyntPåAndreÅretMedUnntak(mottattDato, sisteSak)
        val flereSignifikanteSaker = harFlereSignifikanteSaker(signifikanteSaker.saker(), sisteSak)
        val signifikanteVedtakUtoverTypeAap = harSignifikanteVedtakUtoverTypeAap(signifikanteSaker.signifikanteVedtak)

        // Gitt at det både er sendt søknad, og det er tidligere innvilget to år med utvidet kvote
        val behandlesSomNySøknad = when {
            sisteSak == null -> false
            sisteSak.utredesForUfor -> false
            sisteSak.ferdigAvklart -> false
            else -> {
                har11_12Forlengelse // er tidligere forlenget
                        && sakenHarBegyntPåAndreÅretMedUnntak // på andre året
                        && maksdatoNærmerSeg // og utløpet av ytelsen nærmer seg
                        && !signifikanteVedtakUtoverTypeAap && !flereSignifikanteSaker
            }
        }

        val tilstand = tilstandSomString(signifikanteVedtakUtoverTypeAap, flereSignifikanteSaker, maksdatoNærmerSeg
        ) + "sakenHarBegyntPåAndreÅretMedUnntak=$sakenHarBegyntPåAndreÅretMedUnntak,"

        logger.info("JournalpostId $journalpostId kan behandles som ny søknad: $behandlesSomNySøknad, " + "sak: $sisteSak, tilstand: $tilstand")

        return behandlesSomNySøknad
    }

    internal fun sakenHarBegyntPåAndreÅretMedUnntak(
        mottattDato: LocalDate, sisteSak: SakMedSisteVedtakOgMaksdato?
    ): Boolean {
        return if (sisteSak == null) {
            false
        } else {
            val ettOgEtHalvtÅrSiden = mottattDato.minusMonths(18)
            sisteSak.unntaksvilkaarGjelderFra?.isBefore(ettOgEtHalvtÅrSiden) ?: false
        }

    }

}