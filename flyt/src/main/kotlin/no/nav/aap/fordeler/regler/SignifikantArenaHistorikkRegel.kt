package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.gateway.AapInternApiGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import org.slf4j.LoggerFactory

class SignifikantArenaHistorikkRegel(
    val unleashGateway: UnleashGateway
) : Regel<SignifikantArenaHistorikkRegelInput> {

    companion object : RegelFactory<SignifikantArenaHistorikkRegelInput> {
        override val erAktiv = miljøConfig(prod = true, dev = true)

        override fun medDataInnhenting(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): RegelMedInputgenerator<SignifikantArenaHistorikkRegelInput> {
            val unleashGateway = gatewayProvider.provide<UnleashGateway>()
            return RegelMedInputgenerator(
                SignifikantArenaHistorikkRegel(unleashGateway),
                SignifikantArenaHistorikkRegelInputGenerator(gatewayProvider)
            )
        }
    }

    override fun vurder(input: SignifikantArenaHistorikkRegelInput): Boolean {
        val gradualRollOutToggleIsOff = !unleashGateway.isEnabled(
            PostmottakFeature.AktiverSignifikantArenaHistorikkRegel,
            input.person
        )
        if (gradualRollOutToggleIsOff) {
            return true // regelen er ikke aktiv
        }

        return !input.harSignifikantHistorikkIAAPArena
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class SignifikantArenaHistorikkRegelInputGenerator(private val gatewayProvider: GatewayProvider) :
    InputGenerator<SignifikantArenaHistorikkRegelInput> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun generer(input: RegelInput): SignifikantArenaHistorikkRegelInput {
        val unleashGateway = gatewayProvider.provide(UnleashGateway::class)
        val prodTestErEnabled = unleashGateway.isEnabled(
            PostmottakFeature.AktiverSignifikantArenaHistorikkRegel, // TODO nytt navn
            input.person
        )

        // Hvis toggle er av, hopper vi over kallet mot Arena og sier det er OK
        if (!prodTestErEnabled) {
            return SignifikantArenaHistorikkRegelInput(true, input.person)
        }

        // Ellers gjør kallet mot Arena via AAP Intern API

        val apiInternGateway = gatewayProvider.provide(AapInternApiGateway::class)
        val response = apiInternGateway.harSignifikantHistorikkIAAPArena(input.person, input.mottattDato)
        if (response.harSignifikantHistorikk) {
            logger.info(
                "Personen har signifikant historikk i AAP-Arena: " +
                        "saker=${response.signifikanteSaker}, journalpostId=${input.journalpostId}"
            )
        } else {
            logger.info("Personen har /IKKE/ signifikant historikk i AAP-Arena, journalpostId=${input.journalpostId}")
        }
        return SignifikantArenaHistorikkRegelInput(response.harSignifikantHistorikk, input.person)
    }
}

data class SignifikantArenaHistorikkRegelInput(
    val harSignifikantHistorikkIAAPArena: Boolean,
    val person: Person
)