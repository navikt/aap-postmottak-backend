package no.nav.aap.fordeler.regler

import kotlinx.coroutines.runBlocking
import no.nav.aap.fordeler.arena.ArenaService
import no.nav.aap.fordeler.regler.ArenaHistorikkRegel.Companion.metrikkerForArenaHistorikk
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.PrometheusProvider.Companion.prometheus
import no.nav.aap.postmottak.begrensetInntakTilKelvin
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.personFinnesIAapArenaTeller
import no.nav.aap.postmottak.resultatAvSignifikantArenaHistorikkFilterTeller
import no.nav.aap.postmottak.signifikantArenaHistorikkTeller
import no.nav.aap.postmottak.tellAntallKantIKantDetektert
import no.nav.aap.postmottak.tellAntallMaksUtvidetKvoteSnartOppbrukt
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import org.slf4j.LoggerFactory

class ArenaHistorikkRegel : Regel<ArenaHistorikkRegelInput> {

    companion object : RegelFactory<ArenaHistorikkRegelInput> {
        override val erAktiv = miljøConfig(prod = true, dev = true)

        override fun medDataInnhenting(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): RegelMedInputgenerator<ArenaHistorikkRegelInput> {
            return RegelMedInputgenerator(
                ArenaHistorikkRegel(),
                ArenaHistorikkRegelInputGenerator(gatewayProvider)
            )
        }

        internal fun metrikkerForArenaHistorikk(
            harArenaHistorikk: Boolean,
            harSignifikantArenaHistorikk: Boolean,
            begrensetInntakTilKelvin: Boolean
        ) {
            prometheus.personFinnesIAapArenaTeller(harArenaHistorikk)
                .increment()

            prometheus.signifikantArenaHistorikkTeller(harSignifikantArenaHistorikk)
                .increment()

            if (!harSignifikantArenaHistorikk) {
                // Ville blitt fordelt til Kelvin hvis ikke begrensning på inntaket
                prometheus.begrensetInntakTilKelvin(begrensetInntakTilKelvin)
                    .increment()
            }

            if (harArenaHistorikk) {
                prometheus.resultatAvSignifikantArenaHistorikkFilterTeller(harSignifikantArenaHistorikk)
                    .increment()
            }

        }
    }

    override fun vurder(input: ArenaHistorikkRegelInput): Boolean {
        // TODO: Dersom vi skal ha en mildere regel for Arena-historikk må AvklarSakSteg oppdateres */
        return !input.harSignifikantHistorikkIAAPArena
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class ArenaHistorikkRegelInputGenerator(private val gatewayProvider: GatewayProvider) :
    InputGenerator<ArenaHistorikkRegelInput> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun generer(input: RegelInput): ArenaHistorikkRegelInput {
        val arena = gatewayProvider.provide(ArenaoppslagGateway::class)
        val unleashGateway = gatewayProvider.provide(UnleashGateway::class)
        val innenforProsentenSomVurderesForKelvin = unleashGateway.isEnabled(
            PostmottakFeature.BegrensetFordelingTilKelvin,
            input.person.identifikator.toString() // gradual rollout er sticky på userId
        )

        val (historikk, signifikantHistorikk) = runBlocking {
            val historikk = arena.harHistorikk(input.person)
            val signifikantHistorikk = arena.harSignifikantHistorikk(input.person, input.mottattDato)
            historikk to signifikantHistorikk
        }

        val harSignifikantArenaHistorikk = signifikantHistorikk.harSignifikantHistorikk
        metrikkerForArenaHistorikk(
            historikk,
            harSignifikantArenaHistorikk,
            innenforProsentenSomVurderesForKelvin
        )

        if (harSignifikantArenaHistorikk) {
            logger.info(
                "Personen har signifikant historikk i AAP-Arena: " +
                        "saker=${signifikantHistorikk}, journalpostId=${input.journalpostId}"
            )
            runCatching {
                // Måles kun, påvirker ikke funksjonaliteten
                runBlocking {
                    val arenaService = ArenaService(gatewayProvider)
                    val maksKvoteSnartOppbrukt =
                        arenaService.kanFordelesAutomatiskPga11_12_erMakset(
                            input.person, input.mottattDato,
                            signifikantHistorikk
                        )
                    prometheus.tellAntallMaksUtvidetKvoteSnartOppbrukt(maksKvoteSnartOppbrukt).increment()

                    if (!maksKvoteSnartOppbrukt) {
                        val skalManueltFordeles = arenaService.skalManueltFordeles(
                            input.person, input.mottattDato,
                            signifikantHistorikk
                        )
                        prometheus.tellAntallKantIKantDetektert(skalManueltFordeles).increment()
                    }
                }
            }

        } else {
            logger.info(
                "Personen har /IKKE/ signifikant historikk i AAP-Arena: " +
                        "journalpostId=${input.journalpostId}"
            )
        }


        // Guide til å sette prosent-verdien i Unleash:
        // 62.5% er taket for hvor mye som er lov å ta inn til Kelvin per nå (tall fra metabase 21. mai).
        // Vi vil ta inn regler som øker inntaket med 2%. Si for sikkerhets skyld med 2.5%.
        // Da må vi i tillegg redusere med samme tall, altså ned til 60%.
        // Vi ønsker da å redusere prosenten fra 100 til 60/62.5 % = 96%.
        val resultat = if (innenforProsentenSomVurderesForKelvin) harSignifikantArenaHistorikk else true

        return ArenaHistorikkRegelInput(resultat, input.person)
    }

}

data class ArenaHistorikkRegelInput(
    val harSignifikantHistorikkIAAPArena: Boolean,
    val person: Person
)