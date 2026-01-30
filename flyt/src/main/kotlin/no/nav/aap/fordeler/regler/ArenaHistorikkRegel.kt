package no.nav.aap.fordeler.regler

import no.nav.aap.fordeler.regler.ArenaHistorikkRegel.Companion.tellArenaHistorikk
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.personFinnesIArena
import no.nav.aap.postmottak.gateway.AapInternApiGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
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

        internal fun tellArenaHistorikk(harSignifikantHistorikk: Boolean) {
            PrometheusProvider.prometheus.personFinnesIArena(harSignifikantHistorikk).increment()
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

        val personenFinnesIAapArena = apiInternGateway.harAapSakIArena(input.person).eksisterer

        // Hvis det opprinnelige filteret finner en person, spør vi også det nye filteret dersom det er enabled.
        // Dette gir en gradual rollout av det nye filteret, som en økning av saker oppå det gamle filteret.
        if (personenFinnesIAapArena && nyttFilterAktivt) {
            val nyttFilter = apiInternGateway.harSignifikantHistorikkIAAPArena(input.person, input.mottattDato)
            if (nyttFilter.harSignifikantHistorikk) {
                logger.info(
                    "Personen har signifikant historikk i AAP-Arena: " +
                            "saker=${nyttFilter.signifikanteSaker}, journalpostId=${input.journalpostId}"
                )
            } else {
                logger.info(
                    "Personen har /IKKE/ signifikant historikk i AAP-Arena: " +
                            "journalpostId=${input.journalpostId}"
                )
            }

            return ArenaHistorikkRegelInput(
                nyttFilter.harSignifikantHistorikk,
                input.person
            ).also {
                tellArenaHistorikk(nyttFilter.harSignifikantHistorikk)
            }
        }

        return ArenaHistorikkRegelInput(personenFinnesIAapArena, input.person).also {
            tellArenaHistorikk(personenFinnesIAapArena)
        }
    }
}

data class ArenaHistorikkRegelInput(
    val harSignifikantHistorikkIAAPArena: Boolean,
    val person: Person
)