package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.dbstuff.DbConnection

class BehandlingService(connection: DbConnection) {

    private val behandlingRepository = BehandlingRepository

    fun hent(behandlingId: Long): Behandling {
        return behandlingRepository.hent(behandlingId)
    }
}