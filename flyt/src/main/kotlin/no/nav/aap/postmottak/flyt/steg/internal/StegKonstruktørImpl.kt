package no.nav.aap.behandlingsflyt.flyt.steg.internal

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.StegKonstruktør

class StegKonstruktørImpl(private val connection: DBConnection) : StegKonstruktør {
    override fun konstruer(steg: FlytSteg): BehandlingSteg {
        return steg.konstruer(connection)
    }

    override fun markerSavepoint() {
        connection.markerSavepoint()
    }
}