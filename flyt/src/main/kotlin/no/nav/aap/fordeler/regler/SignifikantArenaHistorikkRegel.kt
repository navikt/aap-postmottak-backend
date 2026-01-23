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
        override val erAktiv = miljøConfig(prod = false, dev = true)

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
    private val secureLog = LoggerFactory.getLogger("team-logs")

    override fun generer(input: RegelInput): SignifikantArenaHistorikkRegelInput {
        val apiInternGateway = gatewayProvider.provide(AapInternApiGateway::class)
        val response = apiInternGateway.harSignifikantHistorikkIAAPArena(input.person, input.mottattDato)
        if (response.harSignifikantHistorikk) {
            secureLog.info(
                "Personen har signifikant historikk i AAP-Arena: " +
                        "saker=${response.signifikanteSaker}, ident=${input.person.identifikator}"
            )
        } else {
            secureLog.info("Personen har /IKKE/ signifikant historikk i AAP-Arena, ident=${input.person.identifikator}")
        }
        return SignifikantArenaHistorikkRegelInput(response.harSignifikantHistorikk, input.person)
    }
}

data class SignifikantArenaHistorikkRegelInput(
    val harSignifikantHistorikkIAAPArena: Boolean,
    val person: Person
)