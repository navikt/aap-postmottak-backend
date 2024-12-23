package no.nav.aap.postmottak.journalpostogbehandling.behandling

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.kontrakt.behandling.Status

interface BehandlingFlytRepository: Repository {
    fun oppdaterBehandlingStatus(behandlingId: BehandlingId, status: Status)
    fun loggBesøktSteg(behandlingId: BehandlingId, tilstand: StegTilstand)
}