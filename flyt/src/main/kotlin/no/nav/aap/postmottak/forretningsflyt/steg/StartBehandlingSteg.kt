package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.kontrakt.steg.StegType


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
        return Fullført
    }



}
