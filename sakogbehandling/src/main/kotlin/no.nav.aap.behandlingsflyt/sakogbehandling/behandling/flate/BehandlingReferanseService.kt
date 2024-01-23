package no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate

import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import java.util.*

class BehandlingReferanseService(val connection: DBConnection) {

    fun behandling(req: BehandlingReferanse): Behandling {
        val eksternReferanse: UUID
        try {
            eksternReferanse = req.ref()
        } catch (exception: IllegalArgumentException) {
            throw ElementNotFoundException()
        }

        return behandlingRepository(connection).hent(eksternReferanse)
    }
}