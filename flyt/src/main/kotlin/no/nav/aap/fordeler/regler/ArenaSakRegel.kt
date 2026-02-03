package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.gateway.AapInternApiGateway

class ArenaSakRegel : Regel<ArenaSakRegelInput> {
    companion object : RegelFactory<ArenaSakRegelInput> {
        override val erAktiv = miljøConfig(prod = true, dev = true)
        override fun medDataInnhenting(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) =
            RegelMedInputgenerator(
                ArenaSakRegel(),
                ArenaSakRegelInputGenerator(gatewayProvider)
            )
    }

    override fun vurder(input: ArenaSakRegelInput) = input.personEksistererIAAPArena

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class ArenaSakRegelInputGenerator(private val gatewayProvider: GatewayProvider) : InputGenerator<ArenaSakRegelInput> {
    override fun generer(input: RegelInput): ArenaSakRegelInput {
        val eksistererIArena = gatewayProvider.provide<AapInternApiGateway>().harAapSakIArena(input.person).eksisterer
        return ArenaSakRegelInput(eksistererIArena)
    }
}

data class ArenaSakRegelInput(
    val personEksistererIAAPArena: Boolean,
)