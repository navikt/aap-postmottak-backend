package no.nav.aap.postmottak.flyt.flate.visning

import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe

class DynamiskStegGruppeVisningService(private val repositoryProvider: RepositoryProvider) {

    private val utledere = mutableMapOf<StegGruppe, StegGruppeVisningUtleder>()

    init {
        StegGruppeVisningUtleder::class.sealedSubclasses.forEach { utleder ->
            val visningUtleder = utleder.constructors
                .find { it.parameters.singleOrNull()?.type?.classifier == RepositoryProvider::class }!!
                .call(repositoryProvider)
            utledere[visningUtleder.gruppe()] = visningUtleder
        }
    }

    fun skalVises(gruppe: StegGruppe, behandlingId: BehandlingId): Boolean {
        if (!gruppe.skalVises) {
            return false
        }
        if (gruppe.obligatoriskVisning) {
            return true
        }

        val utleder = utledere.getValue(gruppe)

        return utleder.skalVises(behandlingId)
    }
}