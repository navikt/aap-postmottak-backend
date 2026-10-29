package no.nav.aap.fordeler.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HistorikkStatusDetaljResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteSakDetaljer
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import org.jetbrains.annotations.TestOnly
import org.slf4j.LoggerFactory
import java.time.LocalDate

@Suppress("MagicNumber")
class ArenaService(gatewayProvider: GatewayProvider) {
    private val arena = gatewayProvider.provide(ArenaoppslagGateway::class)
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val antallUkerFørUtløpMaksimalt = 26L // hvor mange uker før utløp av ytelsen bruker kan søke

        @TestOnly
        fun hentMaksAntallUkerFørUtløpMaksimalt(): Long {
            return antallUkerFørUtløpMaksimalt
        }
    }


    suspend fun skalManueltFordeles(
        søker: Person, mottattDato: LocalDate, journalpostId: Long
    ): Boolean {
        val historikk = hentSisteVedtakMedEffektivMaksdato(søker, mottattDato)
        val sisteSak = historikk.sisteSakOgVedtak

        val maksdatoNærmerSeg = maksdatoNærmerSeg(sisteSak, mottattDato)

        // 11-12 status
        val utfallAv11_12vurderingErKjent = sisteSak?.unntaksvilkaarInnvilget != null
        // sjekk om det er registrert nytt vedtak som bærer 11-12 vurderingen, etter ny prosedyre
        val eget11_12vedtak = historikk.sisteSakOgVedtak?.vedtakene?.find {
                        it.vedtaktypeKode == "O"
                    && it.rettighetkode == "AAP"
                    && it.utfallkode == "NEI"
                    && it.fraOgMed?.isAfter(sisteSak?.sisteVedtak?.til) == true
            // TODO krev også at det har registrert vedtaksfakta med unntak nei
            // ha også med vedtakId slik at vi kan debugge ved behov
            // og ha med aktfasekode, må være == AU eller UA
        }

        val tilManuellFordeling = when {
            // Bruker har valgt å sende en ny søknad om AAP og ..
            sisteSak == null -> false // maxdato er ikke definert
            !maksdatoNærmerSeg -> false
            sisteSak.utredesForUfor() -> false
            sisteSak.erFerdigAvklart() -> false
            sisteSak.erSykepengeErstatning() -> false
            sisteSak.unntaksvilkaarInnvilget != null -> false // 11-12 er allerede vurdert
            else -> {
                // maksdato nærmer seg og spesial-situasjonene over treffer ikke
                true
            }
        }
        val logmsg: StringBuilder = StringBuilder()
        logmsg.append("maksdatoNærmerSeg=$maksdatoNærmerSeg,")
        logmsg.append("unntaksvilkaarInnvilget=${sisteSak?.unntaksvilkaarInnvilget}")
        val tilstand = logmsg.toString()

        logger.info("Journalpost $journalpostId er 'kant-i-kant': $tilManuellFordeling, sak: $sisteSak, tilstand: $tilstand")

        return tilManuellFordeling

    }

    suspend fun kanFordelesAutomatiskTilKelvinPga11_12_erMakset(
        søker: Person, mottattDato: LocalDate, journalpostId: Long
    ): Boolean {
        val historikk = hentSisteVedtakMedEffektivMaksdato(søker, mottattDato)
        val sisteSak = historikk.sisteSakOgVedtak
        val maksdatoNærmerSeg = maksdatoNærmerSeg(sisteSak, mottattDato)
        val sakenHarBegyntPåAndreÅretMedUnntak = sakenHarBegyntPåAndreÅretMedUnntak(mottattDato, sisteSak)
        val unntakErInnvilgetiFremtiden =
            sisteSak?.unntaksvilkaarInnvilget == true && sisteSak.unntaksvilkaarGjelderFra?.isAfter(mottattDato) ?: false

        val behandlesSomNySøknad = when {
            // Bruker har valgt å sende en ny søknad om AAP og ..
            sisteSak == null -> false // maxdato er ikke definert
            !maksdatoNærmerSeg -> false
            sisteSak.utredesForUfor() -> false
            sisteSak.erFerdigAvklart() -> false
            sisteSak.erSykepengeErstatning() -> false
            unntakErInnvilgetiFremtiden -> false
            sisteSak.unntaksvilkaarIkkeOppfylt() -> true // 11-12 er vurdert til "Nei"
            else -> {
                sisteSak.unntaksvilkaarInnvilget == true // saken er tidligere forlenget
                        && sakenHarBegyntPåAndreÅretMedUnntak // er på andre året
            }
        }

        val tilstand = run {
            val logmsg: StringBuilder = StringBuilder()
            logmsg.append("maksdatoNærmerSeg=$maksdatoNærmerSeg,")
            logmsg.append("unntaksvilkaarInnvilget=${sisteSak?.unntaksvilkaarInnvilget},")
            logmsg.append("sakenHarBegyntPåAndreÅretMedUnntak=$sakenHarBegyntPåAndreÅretMedUnntak")
            logmsg.toString()
        }

        logger.info("JournalpostId $journalpostId kan behandles som ny søknad: $behandlesSomNySøknad, " + "sak: $sisteSak, tilstand: $tilstand")

        return behandlesSomNySøknad
    }

    internal fun harSignifikanteVedtakUtoverTypeAap(signifikanteVedtak: List<ArenaVedtak>): Boolean {
        val rettighetskoder = signifikanteVedtak.map { it.rettighetkode }.toMutableSet()
        rettighetskoder.removeAll(listOf("AAP", "AA115")) // klager, anker og annet gjenstår

        return rettighetskoder.isNotEmpty()
    }

    internal fun harFlereSignifikanteSaker(
        saker: List<Int>, sisteSak: SisteSakDetaljer?
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
        sisteSak: SisteSakDetaljer?, mottattDato: LocalDate
    ): Boolean {

        return sisteSak?.sisteVedtak?.maxdatoAap?.let { maxdato ->
            val maxdatoErIkkePassert = maxdato.isAfter(mottattDato)
            val terskeldato = mottattDato.plusWeeks(antallUkerFørUtløpMaksimalt)

            // Sjekk om søknaden som har en definert maksdato er "kant-i-kant" med forrige søknad:
            (maxdatoErIkkePassert && maxdato <= terskeldato)
        } ?: false
    }

    private suspend fun hentSisteVedtakMedEffektivMaksdato(
        søker: Person,
        mottattDato: LocalDate
    ): HistorikkStatusDetaljResponse {
        return arena.historikkStatusDetalj(søker, mottattDato)
    }

    internal fun sakenHarBegyntPåAndreÅretMedUnntak(
        mottattDato: LocalDate, sisteSak: SisteSakDetaljer?
    ): Boolean {
        return if (sisteSak == null) {
            false
        } else {
            // Har vært innvilget unntaksvilkår for 11-12, og det nærmer seg to år siden den datoen.
            // To år er ytterste grense for 11-12 unntak.
            val nærmerSegToÅr = mottattDato.minusYears(2).plusWeeks(antallUkerFørUtløpMaksimalt)
            sisteSak.unntaksvilkaarGjelderFra?.isBefore(nærmerSegToÅr) ?: false
        }

    }

}