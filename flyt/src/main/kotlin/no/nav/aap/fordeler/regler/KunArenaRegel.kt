package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class KunArenaRegel : Regel<Unit> {
    companion object : RegelFactory<Unit> {
        override val erAktiv = miljøConfig(prod = false, dev = false)
        override fun medDataInnhenting(repositoryProvider: RepositoryProvider?, gatewayProvider: GatewayProvider?) =
            RegelMedInputgenerator(KunArenaRegel(), KunArenaRegelInputGenerator())
    }

    override fun vurder(input: Unit) = false

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class KunArenaRegelInputGenerator : InputGenerator<Unit> {
    override fun generer(input: RegelInput) {}
}

