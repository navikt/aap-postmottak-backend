package no.nav.aap.postmottak.flyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.kontrakt.steg.StegType

interface FlytSteg {
    fun konstruer(connection: DBConnection): BehandlingSteg

    fun type(): StegType
}
