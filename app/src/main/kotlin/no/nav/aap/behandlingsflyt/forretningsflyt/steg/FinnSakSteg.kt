package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType


class FinnSakSteg private constructor(): BehandlingSteg {
    companion object: FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return FinnSakSteg()
        }

        override fun type(): StegType {
            return StegType.FINN_SAK
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        return StegResultat()
    }
}