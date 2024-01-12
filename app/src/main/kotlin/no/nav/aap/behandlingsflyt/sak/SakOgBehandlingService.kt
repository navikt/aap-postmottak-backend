package no.nav.aap.behandlingsflyt.sak

import no.nav.aap.behandlingsflyt.behandling.EndringType
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.behandling.Årsak
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class SakOgBehandlingService(connection: DBConnection) {

    private val sakRepository = sakRepository(connection)
    private val behandlingRepository = behandlingRepository(connection)

    fun finnEnRelevantBehandling(key: Saksnummer): BehandlingId {
        val sak = sakRepository.hent(key)

        val sisteBehandlingOpt = behandlingRepository.finnSisteBehandlingFor(sak.id)

        val sisteBehandling = if (sisteBehandlingOpt != null && !sisteBehandlingOpt.status().erAvsluttet()) {
            sisteBehandlingOpt
        } else {
            // Har ikke behandling så oppretter en
            behandlingRepository.opprettBehandling(
                sak.id,
                listOf(Årsak(EndringType.MOTTATT_SØKNAD))
            ) // TODO: Reeltsett oppdatere denne
        }
        return sisteBehandling.id
    }
}