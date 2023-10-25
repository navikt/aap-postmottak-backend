package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.ElementNotFoundException
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse
import java.util.*

object BehandlingReferanseService {

    fun behandling(req: BehandlingReferanse): Behandling {
        val eksternReferanse: UUID
        try {
            eksternReferanse = req.ref()
        } catch (exception: IllegalArgumentException) {
            throw ElementNotFoundException()
        }

        return BehandlingTjeneste.hent(eksternReferanse)
    }
}