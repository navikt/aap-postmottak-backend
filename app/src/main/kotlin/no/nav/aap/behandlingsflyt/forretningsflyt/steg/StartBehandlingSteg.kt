package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(StartBehandlingSteg::class.java)

class StartBehandlingSteg private constructor() : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        log.info("Treffer Start behandling steg")
        /* TODO forsøk å automatisk utrede dokument type
        *  Hvis vi automatisk kan si at brevet skal til AAP til FinnSak
        *  Hvis ikke send til Grovkategorisering
        */
        return StegResultat(listOf(Definisjon.GROVKATEGORISER_DOKUMENT))
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return StartBehandlingSteg()
        }

        override fun type(): StegType {
            return StegType.START_BEHANDLING
        }
    }
}
