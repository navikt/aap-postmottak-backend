package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

class BehandlingService(connection: DBConnection) {

    private val behandlingRepository = behandlingRepository(connection)

    fun hent(behandlingId: BehandlingId): Behandling {
        return behandlingRepository.hent(behandlingId)
    }
}