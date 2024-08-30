package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(JournalføringsstegSteg::class.java)

class JournalføringsstegSteg(private val behandlingRepository: BehandlingRepository) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return JournalføringsstegSteg(BehandlingRepositoryImpl(connection))
        }

        override fun type(): StegType {
            return StegType.ENDERLIG_JOURNALFØRING
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        log.info("Treffer JournalføringsstegSteg")

        return StegResultat()
    }
}