package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

/**
 * Avgjør om en journalpost som ellers ville gått automatisk til Kelvin i stedet skal stoppe for
 * manuell vurdering (avklaringsbehovet VURDER_OPPRETTELSE_AV_SAK), slik at saksbehandler kan velge
 * Arena eller Kelvin.
 *
 * TODO: Selve forretningsregelen (Arena-rettigheter / "kant-i-kant") er ikke implementert enda, så
 * regelen returnerer foreløpig alltid `false`. Når den implementeres hentes data typisk fra
 * [no.nav.aap.postmottak.gateway.AapInternApiGateway].
 */
class TrengerManuellVurderingRegel : Regel<TrengerManuellVurderingInput> {
    companion object : RegelFactory<TrengerManuellVurderingInput> {
        override val erAktiv = miljøConfig(prod = false, dev = true)
        override fun medDataInnhenting(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ) = RegelMedInputgenerator(
            TrengerManuellVurderingRegel(),
            TrengerManuellVurderingInputGenerator()
        )
    }

    override fun vurder(input: TrengerManuellVurderingInput) = input.trengerManuellVurdering

    override fun regelNavn(): String = this::class.simpleName!!
}

class TrengerManuellVurderingInputGenerator : InputGenerator<TrengerManuellVurderingInput> {
    override fun generer(input: RegelInput): TrengerManuellVurderingInput {
        // TODO: Beregn faktisk basert på Arena-historikk (AapInternApiGateway). Foreløpig alltid false.
        return TrengerManuellVurderingInput(trengerManuellVurdering = false)
    }
}

data class TrengerManuellVurderingInput(
    val trengerManuellVurdering: Boolean,
)

