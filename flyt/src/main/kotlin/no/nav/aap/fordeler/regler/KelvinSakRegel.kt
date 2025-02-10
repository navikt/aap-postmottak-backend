package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway

class KelvinSakRegel : Regel<KelvinSakRegelInput> {
    companion object : RegelFactory<KelvinSakRegelInput> {
        override val erAktiv = miljøConfig(prod = false, dev = true)
        override fun medDataInnhenting() =
            RegelMedInputgenerator(KelvinSakRegel(), KelvinSakRegelInputGenerator())
    }

    override fun vurder(input: KelvinSakRegelInput) = input.kelvinSaker.isNotEmpty()

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class KelvinSakRegelInputGenerator : InputGenerator<KelvinSakRegelInput> {
    override fun generer(input: RegelInput): KelvinSakRegelInput {
        val sakerFraKelvin = try {
            GatewayProvider.provide(BehandlingsflytGateway::class).finnSaker(input.person.aktivIdent())
        } catch (e: IkkeFunnetException) {
            emptyList()
        }
        return KelvinSakRegelInput(sakerFraKelvin.map { it.saksnummer })
    }
}

data class KelvinSakRegelInput(
    val kelvinSaker: List<String>
)