package no.nav.aap.fordeler.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HistorikkStatusDetaljResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteSakDetaljer
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import org.jetbrains.annotations.TestOnly
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.time.Period

@Suppress("MagicNumber")
class ArenaService(gatewayProvider: GatewayProvider) {
    private val arena = gatewayProvider.provide(ArenaoppslagGateway::class)
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val antallUkerFørUtløpMaksimalt = 26L // hvor mange uker før utløp av ytelsen bruker kan søke
        private val periodeFørMaksdato = Period.ofMonths(6)
        private val toleranse = Duration.ofDays(5)

        @TestOnly
        fun hentMaksAntallUkerFørUtløpMaksimalt(): Long {
            return antallUkerFørUtløpMaksimalt
        }
    }

    /**
     * @return null hvis vurderingen ikke er gjort enda, true hvis innvilget, false hvis avslått
     */
    suspend fun statusPå11_12Vurdering(
        søker: Person, mottattDato: LocalDate
    ): Boolean? {
        val historikk = hentSisteVedtakMedEffektivMaksdato(søker, mottattDato)

        return historikk.sisteSakOgVedtak?.unntaksvilkaarInnvilget
    }

    suspend fun harKunOppbruktRettighet(
        søker: Person, mottattDato: LocalDate, journalpostId: Long
    ): Boolean {
        val historikk = hentSisteVedtakMedEffektivMaksdato(søker, mottattDato)
        val sisteSak = historikk.sisteSakOgVedtak

        val tilDatoErIkkeLikMaksdato = !erTildatoLikMaksdatoInnenforToleranse(sisteSak)
        val maksdatoErIkkeInnenRimeligTid = !maksDatoNærmerSeg(sisteSak, mottattDato)
        val skalIkkeForlenges = unntakEtter11_12ErVurdertTilNei(historikk)
        val erTildatoLikMaksdatoForUnntakInnenforToleranse = erTildatoLikMaksdatoForUnntakInnenforToleranse(sisteSak)

        val harKunOppbrukt = when {
            // Bruker har valgt å sende en ny søknad om AAP og ..
            sisteSak == null -> false // har ingen løpende eller avsluttet AAP-vedtak i aktivitetsfase UA eller AU
            sisteSak.utredesForUfor() -> false
            sisteSak.erFerdigAvklart() -> false
            sisteSak.erSykepengeErstatning() -> false
            maksdatoErIkkeInnenRimeligTid -> false
            tilDatoErIkkeLikMaksdato -> false
            // tilfellet med å ha brukt opp ordinær kvote og unntaksvilkår for 11-12 ikke er oppfylt
            !skalIkkeForlenges -> true
            else -> {
                // Tilfellet med å ha fått maks to av to år med unntaksvilkår for 11-12
                erTildatoLikMaksdatoForUnntakInnenforToleranse
            }
        }
        val logmsg: StringBuilder = StringBuilder()
        logmsg.append("tilDatoErLikMaksdato=${!tilDatoErIkkeLikMaksdato},")
        logmsg.append("maksdatoErInnenRimeligTid=${!maksdatoErIkkeInnenRimeligTid}")
        logmsg.append("vedtatt at ikke forlenges=${skalIkkeForlenges}")
        logmsg.append("brukt opp fem år=${erTildatoLikMaksdatoForUnntakInnenforToleranse}")
        val tilstand = logmsg.toString()

        logger.info("Journalpost $journalpostId er 'kant-i-kant': $harKunOppbrukt, sak: $sisteSak, tilstand: $tilstand")

        return harKunOppbrukt
    }

    private fun unntakEtter11_12ErVurdertTilNei(historikk: HistorikkStatusDetaljResponse): Boolean {
        val unntaksvilkaarInnvilget = historikk.sisteSakOgVedtak?.unntaksvilkaarInnvilget
        val unntakIkkeInvilget = unntaksvilkaarInnvilget == false
        return unntakIkkeInvilget
    }

    private fun maksDatoNærmerSeg(
        sisteSak: SisteSakDetaljer?,
        mottattDato: LocalDate
    ): Boolean {
        return sisteSak?.sisteVedtak?.maxdatoAap?.minus(periodeFørMaksdato)?.isBefore(mottattDato) == true
    }

    private fun erTildatoLikMaksdatoInnenforToleranse(sisteSak: SisteSakDetaljer?): Boolean {
        return erDatoerInnenforToleranse(
            sisteSak?.sisteVedtak?.til,
            sisteSak?.sisteVedtak?.maxdatoOrdinaer
        )
    }

    private fun erTildatoLikMaksdatoForUnntakInnenforToleranse(sisteSak: SisteSakDetaljer?): Boolean {
        return erDatoerInnenforToleranse(
            sisteSak?.sisteVedtak?.til,
            sisteSak?.sisteVedtak?.maxdatoUnntak
        )
    }

    private fun erDatoerInnenforToleranse(tilDato: LocalDate?, maksdato: LocalDate?): Boolean =
        if (tilDato == null || maksdato == null) {
            false
        } else {
            val avstand = Duration.between(tilDato.atStartOfDay(), maksdato.atStartOfDay()).abs()
            toleranse >= avstand
        }

    @Deprecated("Bruk oppbruktRettighet()")
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