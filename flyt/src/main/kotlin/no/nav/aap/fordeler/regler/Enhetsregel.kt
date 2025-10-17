package no.nav.aap.fordeler.regler

import no.nav.aap.fordeler.EnhetMedOppfølgingsKontor
import no.nav.aap.fordeler.Enhetsutreder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.kontrakt.enhet.GodkjentEnhet
import no.nav.aap.unleash.UnleashGateway
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Enhetsregel::class.java)

class Enhetsregel(
    unleashGateway: UnleashGateway
) : Regel<EnhetsregelInput> {

    companion object : RegelFactory<EnhetsregelInput> {
        override val erAktiv = miljøConfig(prod = true, dev = false)
        override fun medDataInnhenting(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) =
            RegelMedInputgenerator(
                Enhetsregel(gatewayProvider.provide()),
                EnhetsregelInputGenerator(gatewayProvider)
            )
    }

    override fun vurder(input: EnhetsregelInput): Boolean {
        if (input.enheter.norgEnhet == null && input.enheter.oppfølgingsenhet == null) {
            log.info("Fant ikke enheter for person")
        }
        return (input.enheter.oppfølgingsenhet ?: input.enheter.norgEnhet) in GodkjentEnhet.entries.map { it.enhetNr }
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}


class EnhetsregelInputGenerator(private val gatewayProvider: GatewayProvider) : InputGenerator<EnhetsregelInput> {
    override fun generer(input: RegelInput): EnhetsregelInput {
        val enheter = Enhetsutreder.konstruer(gatewayProvider).finnEnhetMedOppfølgingskontor(input.person)
        return EnhetsregelInput(enheter)
    }
}

data class EnhetsregelInput(
    val enheter: EnhetMedOppfølgingsKontor
)