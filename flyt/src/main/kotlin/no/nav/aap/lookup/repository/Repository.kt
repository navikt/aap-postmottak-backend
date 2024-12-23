package no.nav.aap.lookup.repository

import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

/**
 * Marker interface for repository.
 *
 * PS: Hver gang denne implementeres, må også App.kt oppdateres for at implementasjonene
 * skal lastes i [RepositoryRegistry].
 */
interface Repository {
    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {}
}