package no.nav.aap.postmottak.flyt

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.kontrakt.steg.StegType

class GeneriskTestFlytSteg(private val stegType: StegType) : FlytSteg {
    override fun konstruer(connection: DBConnection): BehandlingSteg {
        return GeneriskTestSteg()
    }

    override fun type(): StegType {
        return stegType
    }
}
