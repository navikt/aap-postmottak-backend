package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory


class StartBehandlingSteg private constructor() : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return StartBehandlingSteg()
        }

        override fun type(): StegType {
            return StegType.START_BEHANDLING
        }
    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        // Obligatorisk startsteg for alle flyter
        return StegResultat()
    }



}
