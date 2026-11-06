package no.nav.aap.fordeler.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.HistorikkStatusDetaljResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteSakDetaljer
import no.nav.aap.fordeler.arena.ArenaService.OppbruktRettighetResultat.HarIkkeOppbruktRettighet.IkkeRelevantSøknad
import no.nav.aap.fordeler.arena.ArenaService.OppbruktRettighetResultat.HarIkkeOppbruktRettighet.VedtattForlenget
import no.nav.aap.fordeler.arena.ArenaService.OppbruktRettighetResultat.HarOppbruktRettighet.HarBruktOppForlengelsen
import no.nav.aap.fordeler.arena.ArenaService.OppbruktRettighetResultat.HarOppbruktRettighet.VedtattAtIkkeForlenges
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
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
        private val periodeFørMaksdato = Period.ofWeeks(26)
        private val toleranseForLikeDatoer = 5.days
    }

    /**
     * @return null hvis vurderingen ikke er gjort enda, true hvis innvilget, false hvis avslått
     */
    suspend fun statusPå11_12Vurdering(
        søker: Person, mottattDato: LocalDate
    ): Boolean? {
        val historikk = hentHistorikkDetalj(søker, mottattDato)

        return historikk.sisteSakOgVedtak?.unntaksvilkaarInnvilget
    }

    sealed class OppbruktRettighetResultat private constructor() {

        fun skalTilKelvin() = this is HarOppbruktRettighet

        sealed class HarOppbruktRettighet private constructor() : OppbruktRettighetResultat() {
            // Personen har fått forlengelse, og den er brukt helt opp
            data object HarBruktOppForlengelsen : HarOppbruktRettighet()

            // Personen har brukt opp ordinær kvote, men har ikke fått forlengelse
            data object VedtattAtIkkeForlenges : HarOppbruktRettighet()
        }

        sealed class HarIkkeOppbruktRettighet private constructor() : OppbruktRettighetResultat() {
            // Søknaden er ikke kant-i-kant med vedtakene i Arena
            data object IkkeRelevantSøknad : HarIkkeOppbruktRettighet()

            // Søknaden er vedtatt forlenget, men ikke brukt opp
            data object VedtattForlenget : HarIkkeOppbruktRettighet()
        }
    }

    /*   Vurderer om søker har oppbrukt rettighet til AAP, basert på siste vedtak i Arena.
        Returnerer et resultat som viser om historikken gjør søknaden relevant for behandling eller ikke.
     */
    suspend fun harOppbruktRettighet(
        søker: Person, mottattDato: LocalDate, journalpostId: Long
    ): OppbruktRettighetResultat {
        val historikk = hentHistorikkDetalj(søker, mottattDato)
        val sisteSak = historikk.sisteSakOgVedtak
        if (sisteSak == null || !sisteSak.erUnderArbeidsAvklaring() && !sisteSak.erUnderArbeidsUtprøving()) {
            // Søker har ingen løpende eller avsluttet AAP-vedtak i aktivitetsfase UA eller AU
            logger.info("Søkeren i journalpost $journalpostId har ingen aktuell sak i Arena, vurderer som ikke relevant")
            return IkkeRelevantSøknad
        } else if (harVedtakIArenaSomStarterIFremtiden(sisteSak, mottattDato)) {
            // Søker har allerede fått innvilget AAP i Arena for fremtidig periode
            logger.info("Søkeren i journalpost $journalpostId har allerede innvilget AAP i Arena for fremtidig periode, vurderer som ikke relevant")
            return IkkeRelevantSøknad
        }

        val maksdatoErIkkeInnenRimeligTid = !maksDatoNærmerSeg(sisteSak, mottattDato)
        val tilDatoErIkkeLikOrdinærMaksdato = !erTildatoLikMaksdatoInnenforToleranse(sisteSak)
        val tilDatoErLikMaksdatoForUnntak = erTildatoLikMaksdatoForUnntakInnenforToleranse(sisteSak)
        val skalIkkeForlenges = sisteSak.unntaksvilkaarIkkeOppfylt()
        val erAlleredeForlenget = sisteSak.unntaksvilkaarOppfylt()

        val harKunOppbrukt = when {
            // Bruker har valgt å sende en ny søknad om AAP og ..
            maksdatoErIkkeInnenRimeligTid -> IkkeRelevantSøknad // har søkt for tidlig
            erAlleredeForlenget -> if (tilDatoErLikMaksdatoForUnntak) {
                // har fått forlengelse og brukt den helt opp når ytelsen i Arena utløper
                HarBruktOppForlengelsen
            } else {
                // forlenget, men vi er ikke ved utløpet av unntaksperioden
                // feks. søkeren er på år ett av to av forlengelsen
                VedtattForlenget
            }
            // vedtaket i Arena går ikke til maksdato for ordinær ytelse, ytelsen kan derfor ikke være oppbrukt:
            tilDatoErIkkeLikOrdinærMaksdato -> IkkeRelevantSøknad
            // ordinær kvote er brukt opp og unntaksvilkårene for 11-12 er /IKKE/ oppfylt:
            skalIkkeForlenges -> VedtattAtIkkeForlenges
            // andre tilfeller, som ikke oppfyller kriteriene:
            else -> IkkeRelevantSøknad
        }
        val logmsg: StringBuilder = StringBuilder()
        logmsg.append("tilDatoErLikOrdinærMaksdato=${!tilDatoErIkkeLikOrdinærMaksdato}, ")
        logmsg.append("maksdatoErInnenRimeligTid=${!maksdatoErIkkeInnenRimeligTid}, ")
        logmsg.append("vedtatt at ikke forlenges=${skalIkkeForlenges}, ")
        logmsg.append("er forlenget=${erAlleredeForlenget}, ")
        logmsg.append("brukt opp fem år=${tilDatoErLikMaksdatoForUnntak}")
        val tilstand = logmsg.toString()

        logger.info("Journalpost $journalpostId er 'kant-i-kant': ${harKunOppbrukt::class.simpleName}, sak: $sisteSak, tilstand: $tilstand")

        return harKunOppbrukt
    }

    private fun harVedtakIArenaSomStarterIFremtiden(
        sisteSak: SisteSakDetaljer, mottattDato: LocalDate
    ): Boolean = sisteSak.sisteVedtak.fra?.isAfter(mottattDato) == true

    private fun maksDatoNærmerSeg(
        sisteSak: SisteSakDetaljer?, mottattDato: LocalDate
    ): Boolean {
        // bruker den siste av ordinær eller unntaks-maksdatoen for å vurdere om søknaden er relevant
        return sisteSak?.sisteVedtak?.maxdatoAap?.isBefore(mottattDato + periodeFørMaksdato) == true
    }

    private fun erTildatoLikMaksdatoInnenforToleranse(sisteSak: SisteSakDetaljer?): Boolean {
        return erDatoerInnenforToleranse(
            sisteSak?.sisteVedtak?.til, sisteSak?.sisteVedtak?.maxdatoOrdinaer
        )
    }

    private fun erTildatoLikMaksdatoForUnntakInnenforToleranse(sisteSak: SisteSakDetaljer?): Boolean {
        return erDatoerInnenforToleranse(
            sisteSak?.sisteVedtak?.til, sisteSak?.sisteVedtak?.maxdatoUnntak
        )
    }

    private fun erDatoerInnenforToleranse(tilDato: LocalDate?, maksdato: LocalDate?): Boolean =
        if (tilDato == null || maksdato == null) {
            false
        } else {
            val dagerMellom = ChronoUnit.DAYS.between(tilDato, maksdato).days
            dagerMellom.absoluteValue <= toleranseForLikeDatoer
        }

    private suspend fun hentHistorikkDetalj(
        søker: Person, mottattDato: LocalDate
    ): HistorikkStatusDetaljResponse {
        return arena.historikkStatusDetalj(søker, mottattDato)
    }

}