package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType

class KategoriserDokumentSteg private constructor(): BehandlingSteg {
    companion object: FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return KategoriserDokumentSteg()
        }

        override fun type(): StegType {
            return StegType.KATEGORISER_DOKUMENT
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        return StegResultat()
    }
}