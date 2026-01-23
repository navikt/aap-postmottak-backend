package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway

class KelvinSakRegel : Regel<KelvinSakRegelInput> {
    companion object : RegelFactory<KelvinSakRegelInput> {
        override val erAktiv = miljøConfig(prod = true, dev = true)
        override fun medDataInnhenting(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) =
            RegelMedInputgenerator(
                KelvinSakRegel(),
                KelvinSakRegelInputGenerator(gatewayProvider)
            )
    }

    override fun vurder(input: KelvinSakRegelInput) = input.kelvinSaker.isNotEmpty()

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class KelvinSakRegelInputGenerator(private val gatewayProvider: GatewayProvider) : InputGenerator<KelvinSakRegelInput> {
    override fun generer(input: RegelInput): KelvinSakRegelInput {
        val sakerFraKelvin = gatewayProvider.provide<BehandlingsflytGateway>().finnSaker(input.person.aktivIdent())
        return KelvinSakRegelInput(sakerFraKelvin.map { it.saksnummer })
    }
}

data class KelvinSakRegelInput(
    val kelvinSaker: List<String>
)