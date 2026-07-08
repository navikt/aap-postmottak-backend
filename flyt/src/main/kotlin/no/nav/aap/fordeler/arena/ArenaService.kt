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
        val sisteSak = hentSisteVedtakMedEffektivMaksdato(søker)

        val maksdatoNærmerSeg = maksdatoNærmerSeg(sisteSak, mottattDato)
        val flereSignifikanteSaker = harFlereSignifikanteSaker(signifikantHistorikk.saker(), sisteSak)
        val signifikanteVedtakUtoverTypeAap =
            harSignifikanteVedtakUtoverTypeAap(signifikantHistorikk.signifikanteVedtak)

        // Dersom 11-12 allerede er innvilget for et kommende nytt år skal den ikke til manuell fordeling.
        // Den situasjonen gjenspeiles i maxdatoAap, og maxdatoAap vil da være forbi `terskeldato`.
        // Derfor er 11-12 situasjonen også håndtert, selv om det ikke er en eksplisitt sjekk for den.

        val tilManuellFordeling = when {
            // Bruker har valgt å sende en ny søknad om AAP og ..
            sisteSak == null -> false // maxdato er ikke definert
            !maksdatoNærmerSeg -> false
            flereSignifikanteSaker -> false
            signifikanteVedtakUtoverTypeAap -> false
            sisteSak.utredesForUfor() -> false
            sisteSak.erFerdigAvklart() -> false
            sisteSak.erSykepengeErstatning() -> false

            else -> {
                // maksdato nærmer seg og spesial-situasjonene over treffer ikke
                true
            }
        }
        val tilstand = tilstandSomString(
            signifikanteVedtakUtoverTypeAap,
            flereSignifikanteSaker,
            maksdatoNærmerSeg,
            sisteSak?.unntaksvilkaarIkkeOppfylt()
        )

        logger.info("Journalpost $journalpostId er 'kant-i-kant': $tilManuellFordeling, sak: $sisteSak, tilstand: $tilstand")

        return tilManuellFordeling

    }

    suspend fun kanFordelesAutomatiskPga11_12_erMakset(
        søker: Person, mottattDato: LocalDate, journalpostId: Long, signifikanteSaker: SignifikantHistorikkResponse
    ): Boolean {
        val sisteSak = hentSisteVedtakMedEffektivMaksdato(søker)
        val maksdatoNærmerSeg = maksdatoNærmerSeg(sisteSak, mottattDato)
        val sakenHarBegyntPåAndreÅretMedUnntak = sakenHarBegyntPåAndreÅretMedUnntak(mottattDato, sisteSak)
        val flereSignifikanteSaker = harFlereSignifikanteSaker(signifikanteSaker.saker(), sisteSak)
        val signifikanteVedtakUtoverTypeAap = harSignifikanteVedtakUtoverTypeAap(signifikanteSaker.signifikanteVedtak)
        val unntakErInnvilgetiFremtiden = sisteSak?.unntaksvilkaarGjelderFra?.isAfter(mottattDato) ?: false

        val behandlesSomNySøknad = when {
            // Bruker har valgt å sende en ny søknad om AAP og ..
            sisteSak == null -> false // maxdato er ikke definert
            !maksdatoNærmerSeg -> false
            flereSignifikanteSaker -> false
            signifikanteVedtakUtoverTypeAap -> false
            sisteSak.utredesForUfor() -> false
            sisteSak.erFerdigAvklart() -> false
            sisteSak.erSykepengeErstatning() -> false
            unntakErInnvilgetiFremtiden -> false
            sisteSak.unntaksvilkaarIkkeOppfylt() -> true // 11-12 er vurdert til "Nei"
            else -> {
                sisteSak.harInnvilget11_12() // saken er tidligere forlenget
                        && sakenHarBegyntPåAndreÅretMedUnntak // er på andre året
            }
        }

        val tilstand = tilstandSomString(
            signifikanteVedtakUtoverTypeAap,
            flereSignifikanteSaker,
            maksdatoNærmerSeg,
            sisteSak?.unntaksvilkaarIkkeOppfylt()
        ) + "sakenHarBegyntPåAndreÅretMedUnntak=$sakenHarBegyntPåAndreÅretMedUnntak,"

        logger.info("JournalpostId $journalpostId kan behandles som ny søknad: $behandlesSomNySøknad, " + "sak: $sisteSak, tilstand: $tilstand")

        return behandlesSomNySøknad
    }

    private fun tilstandSomString(
        signifikanteVedtakUtoverTypeAap: Boolean,
        flereSignifikanteSaker: Boolean,
        maksdatoNærmerSeg: Boolean,
        unntaksvilkaarIkkeOppfylt: Boolean?
    ): String {
        val logmsg: StringBuilder = StringBuilder()
        logmsg.append("signifikanteVedtakUtoverTypeAap=$signifikanteVedtakUtoverTypeAap,")
        logmsg.append("flereSignifikanteSaker=$flereSignifikanteSaker,")
        logmsg.append("maksdatoNærmerSeg=$maksdatoNærmerSeg,")
        logmsg.append("unntaksvilkaarIkkeOppfylt=$unntaksvilkaarIkkeOppfylt,")
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

    private suspend fun hentSisteVedtakMedEffektivMaksdato(søker: Person): SakMedSisteVedtakOgMaksdato? {
        return when (val sisteVedtak = arena.sisteVedtakMedMaksdato(søker.aktivIdent())) {
            null -> null
            else if (sisteVedtak.sakAvsluttet != null // helt avsluttet, kan ikke ha maxdato i nær fremtid
                    || !sisteVedtak.erLopende()) -> sisteVedtak.medUdefinertMaxsdato()

            else -> {
                sisteVedtak // har en gyldig maxdato
            }
        }
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