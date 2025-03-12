package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.AapInternApiGateway

class ArenaHistorikkRegel : Regel<ArenaHistorikkRegelInput> {
    companion object : RegelFactory<ArenaHistorikkRegelInput> {
        override val erAktiv = miljøConfig(prod = false, dev = true)
        override fun medDataInnhenting(connection: DBConnection?) =
            RegelMedInputgenerator(ArenaHistorikkRegel(), ArenaSakRegelInputGenerator())
    }

    override fun vurder(input: ArenaHistorikkRegelInput): Boolean {
        // TODO: Dersom vi skal ha en mildere regel for Arena-historikk må AvklarSakSteg oppdateres */
        return !input.harAapSakIArena
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class ArenaSakRegelInputGenerator : InputGenerator<ArenaHistorikkRegelInput> {
    override fun generer(input: RegelInput): ArenaHistorikkRegelInput {
        val harAapSakIArena = GatewayProvider.provide(AapInternApiGateway::class).harAapSakIArena(input.person)
        return ArenaHistorikkRegelInput(harAapSakIArena.eksisterer)
    }
}

data class ArenaHistorikkRegelInput(
    val harAapSakIArena: Boolean,
)