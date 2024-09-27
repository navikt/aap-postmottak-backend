package no.nav.aap.postmottak.sakogbehandling.behandling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

interface BehandlingFlytRepository {

    fun oppdaterBehandlingStatus(behandlingId: BehandlingId, status: Status)
    fun loggBesøktSteg(behandlingId: BehandlingId, tilstand: StegTilstand)
}

fun BehandlingFlytRepository(connection: DBConnection): BehandlingFlytRepository {
    return BehandlingRepositoryImpl(connection)
}