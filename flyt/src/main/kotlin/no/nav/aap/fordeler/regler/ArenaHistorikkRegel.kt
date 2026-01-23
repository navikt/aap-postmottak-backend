package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.gateway.AapInternApiGateway

class ArenaHistorikkRegel : Regel<ArenaHistorikkRegelInput> {
    companion object : RegelFactory<ArenaHistorikkRegelInput> {
        override fun medDataInnhenting(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) =
            RegelMedInputgenerator(
                ArenaHistorikkRegel(),
                ArenaSakRegelInputGenerator(gatewayProvider)
            )
    }
    override fun erAktiv() = miljøConfig(prod = true, dev = true)

    override fun vurder(input: ArenaHistorikkRegelInput): Boolean {
        // TODO: Dersom vi skal ha en mildere regel for Arena-historikk må AvklarSakSteg oppdateres */
        return !input.harAapSakIArena
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class ArenaSakRegelInputGenerator(private val gatewayProvider: GatewayProvider) :
    InputGenerator<ArenaHistorikkRegelInput> {
    override fun generer(input: RegelInput): ArenaHistorikkRegelInput {
        val harAapSakIArena = gatewayProvider.provide(AapInternApiGateway::class).harAapSakIArena(input.person)
        return ArenaHistorikkRegelInput(harAapSakIArena.eksisterer)
    }
}

data class ArenaHistorikkRegelInput(
    val harAapSakIArena: Boolean,
)