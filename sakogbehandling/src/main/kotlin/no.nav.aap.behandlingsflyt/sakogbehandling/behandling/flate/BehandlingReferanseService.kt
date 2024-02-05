package no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate

import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import java.util.*

class BehandlingReferanseService(val connection: DBConnection) {

    fun behandling(req: BehandlingReferanse): Behandling {
        val eksternReferanse: UUID
        try {
            eksternReferanse = req.ref()
        } catch (exception: IllegalArgumentException) {
            throw ElementNotFoundException()
        }

        return BehandlingRepositoryImpl(connection).hent(eksternReferanse)
    }
}