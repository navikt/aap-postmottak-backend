package no.nav.aap.fordeler.regler

import no.nav.aap.fordeler.regler.ArenaHistorikkRegel.Companion.metrikkerForArenaHistorikk
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.PrometheusProvider.Companion.prometheus
import no.nav.aap.postmottak.gateway.AapInternApiGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.personFinnesIAapArenaTeller
import no.nav.aap.postmottak.resultatAvSignifikantArenaHistorikkFilterTeller
import no.nav.aap.postmottak.signifikantArenaHistorikkTeller
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
            arenaHistorikkenErSignifikant: Boolean
        ) {
            prometheus.personFinnesIAapArenaTeller(harArenaHistorikk)
                .increment()

            prometheus.signifikantArenaHistorikkTeller(arenaHistorikkenErSignifikant)
                .increment()

            if (harArenaHistorikk) {
                prometheus.resultatAvSignifikantArenaHistorikkFilterTeller(arenaHistorikkenErSignifikant)
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
        val apiInternGateway = gatewayProvider.provide(AapInternApiGateway::class)
        val unleashGateway = gatewayProvider.provide(UnleashGateway::class)
        val nyttFilterAktivt = unleashGateway.isEnabled(
            PostmottakFeature.AktiverSignifikantArenaHistorikkRegel,
            input.person.identifikator.toString() // gradual rollout er sticky på userId
        )

        val arenaPerson = apiInternGateway.harAapSakIArena(input.person)
        val signifikantHistorikk = apiInternGateway.harSignifikantHistorikkIAAPArena(input.person, input.mottattDato)
        metrikkerForArenaHistorikk(arenaPerson.eksisterer, signifikantHistorikk.harSignifikantHistorikk)

        if (signifikantHistorikk.harSignifikantHistorikk) {
            logger.info(
                "Personen har signifikant historikk i AAP-Arena: " +
                        "saker=${signifikantHistorikk.signifikanteSaker}, journalpostId=${input.journalpostId}"
            )
        } else {
            logger.info(
                "Personen har /IKKE/ signifikant historikk i AAP-Arena: " +
                        "journalpostId=${input.journalpostId}"
            )
        }

        // Vi gjør gradual rollout av nytt filter
        val resultat = if (nyttFilterAktivt) {
            signifikantHistorikk.harSignifikantHistorikk
        } else {
            arenaPerson.eksisterer
        }

        return ArenaHistorikkRegelInput(resultat, input.person)
    }
}

data class ArenaHistorikkRegelInput(
    val harSignifikantHistorikkIAAPArena: Boolean,
    val person: Person
)