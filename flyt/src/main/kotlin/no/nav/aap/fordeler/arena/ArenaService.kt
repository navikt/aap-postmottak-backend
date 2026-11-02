package no.nav.aap.fordeler.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HistorikkStatusDetaljResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteSakDetaljer
import no.nav.aap.fordeler.arena.ArenaService.OppbruktRettighetResultat.HarIkkeOppbruktRettighet.IkkeRelevantSøknad
import no.nav.aap.fordeler.arena.ArenaService.OppbruktRettighetResultat.HarIkkeOppbruktRettighet.VedtattForlenget
import no.nav.aap.fordeler.arena.ArenaService.OppbruktRettighetResultat.HarOppbruktRettighet.HarBruktOppForlengelsen
import no.nav.aap.fordeler.arena.ArenaService.OppbruktRettighetResultat.HarOppbruktRettighet.VedtattAtIkkeForlenges
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import org.jetbrains.annotations.TestOnly
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.days

@Suppress("MagicNumber")
class ArenaService(gatewayProvider: GatewayProvider) {
    private val arena = gatewayProvider.provide(ArenaoppslagGateway::class)
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val antallUkerFørUtløpMaksimalt = 26L // hvor mange uker før utløp av ytelsen bruker kan søke
        private val periodeFørMaksdato = Period.ofWeeks(26)
        private val toleranse = 5.days

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

    sealed class OppbruktRettighetResultat private constructor() {
        sealed class HarOppbruktRettighet private constructor() : OppbruktRettighetResultat() {
            data object HarBruktOppForlengelsen : HarOppbruktRettighet()
            data object VedtattAtIkkeForlenges : HarOppbruktRettighet()
        }

        sealed class HarIkkeOppbruktRettighet private constructor() : OppbruktRettighetResultat() {
            data object IkkeRelevantSøknad : HarIkkeOppbruktRettighet()
            data object VedtattForlenget : HarIkkeOppbruktRettighet()
        }
    }

    suspend fun harOppbruktRettighet(
        søker: Person, mottattDato: LocalDate, journalpostId: Long
    ): OppbruktRettighetResultat {
        val historikk = hentSisteVedtakMedEffektivMaksdato(søker, mottattDato)
        val sisteSak = historikk.sisteSakOgVedtak

        val tilDatoErIkkeLikOrdinærMaksdato = !erTildatoLikMaksdatoInnenforToleranse(sisteSak)
        val tilDatoErLikMaksdatoForUnntak = erTildatoLikMaksdatoForUnntakInnenforToleranse(sisteSak)
        val maksdatoErIkkeInnenRimeligTid = !maksDatoNærmerSeg(sisteSak, mottattDato)
        val skalIkkeForlenges = unntakEtter11_12Er(historikk) == false
        val erAlleredeForlenget = unntakEtter11_12Er(historikk) == true

        val harKunOppbrukt = when {
            // Bruker har valgt å sende en ny søknad om AAP og ..
            sisteSak == null -> IkkeRelevantSøknad // har ingen løpende eller avsluttet AAP-vedtak i aktivitetsfase UA eller AU
            sisteSak.utredesForUfor() -> IkkeRelevantSøknad
            sisteSak.erFerdigAvklart() -> IkkeRelevantSøknad
            sisteSak.erSykepengeErstatning() -> IkkeRelevantSøknad
            maksdatoErIkkeInnenRimeligTid -> IkkeRelevantSøknad // har søkt for tidlig
            tilDatoErIkkeLikOrdinærMaksdato -> IkkeRelevantSøknad // det er ikke kant-i-kant med ytelsen i Arena
            // tilfellet med å ha brukt opp ordinær kvote og unntaksvilkår for 11-12 ikke er oppfylt
            skalIkkeForlenges -> VedtattAtIkkeForlenges
            erAlleredeForlenget -> VedtattForlenget
            else -> {
                if (tilDatoErLikMaksdatoForUnntak) {
                    // Tilfellet med å ha fått maks to av to år med unntaksvilkår for 11-12
                    HarBruktOppForlengelsen
                } else {
                    IkkeRelevantSøknad
                }
            }
        }
        val logmsg: StringBuilder = StringBuilder()
        logmsg.append("tilDatoErLikMaksdato=${!tilDatoErIkkeLikOrdinærMaksdato},")
        logmsg.append("maksdatoErInnenRimeligTid=${!maksdatoErIkkeInnenRimeligTid}")
        logmsg.append("vedtatt at ikke forlenges=${skalIkkeForlenges}")
        logmsg.append("brukt opp fem år=${tilDatoErLikMaksdatoForUnntak}")
        val tilstand = logmsg.toString()

        logger.info("Journalpost $journalpostId er 'kant-i-kant': $harKunOppbrukt, sak: $sisteSak, tilstand: $tilstand")

        return harKunOppbrukt
    }

    private fun unntakEtter11_12Er(historikk: HistorikkStatusDetaljResponse): Boolean? {
        val unntaksvilkaarInnvilget = historikk.sisteSakOgVedtak?.unntaksvilkaarInnvilget
        return unntaksvilkaarInnvilget
    }

    private fun maksDatoNærmerSeg(
        sisteSak: SisteSakDetaljer?,
        mottattDato: LocalDate
    ): Boolean {
        return sisteSak?.sisteVedtak?.maxdatoAap?.isBefore(mottattDato + periodeFørMaksdato) == true
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
            val dagerMellom = ChronoUnit.DAYS.between(tilDato, maksdato).days.absoluteValue
            dagerMellom <= toleranse
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