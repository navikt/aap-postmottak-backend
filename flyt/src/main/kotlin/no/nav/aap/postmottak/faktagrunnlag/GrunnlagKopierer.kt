package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

class GrunnlagKopierer(private val repositoryProvider: RepositoryProvider) {
    fun overfør(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        require(fraBehandlingId != tilBehandlingId)
        repositoryProvider.provideAlle().forEach { repository ->
            if (repository is no.nav.aap.lookup.repository.Repository) {
                repository.kopier(fraBehandlingId, tilBehandlingId)
            }
        }
    }
}
