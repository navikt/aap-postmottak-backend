package no.nav.aap.fordeler.regler

import no.nav.aap.fordeler.ManuellFordelingRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.Fagsystem

class ManueltOverstyrtTilArenaRegel : Regel<List<Fagsystem>> {
    companion object : RegelFactory<List<Fagsystem>> {
        override fun erAktiv(gatewayProvider: GatewayProvider) = miljøConfig(prod = true, dev = true)
        override fun medDataInnhenting(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) =
            RegelMedInputgenerator(
                ManueltOverstyrtTilArenaRegel(),
                ManueltOverstyrtTilArenaRegelInputGenerator(repositoryProvider)
            )
    }

    override fun vurder(input: List<Fagsystem>): Boolean {
        check(input.size <= 1) {
            "Kan ikke overstyre samme person til flere fagsystemer"
        }
        return input.singleOrNull() == Fagsystem.arena
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class ManueltOverstyrtTilArenaRegelInputGenerator(private val repositoryProvider: RepositoryProvider) :
    InputGenerator<List<Fagsystem>> {
    override fun generer(input: RegelInput): List<Fagsystem> =
        input.person.identer().mapNotNull {
            repositoryProvider.provide<ManuellFordelingRepository>()
                .fordelTilFagsystem(it)
        }
}