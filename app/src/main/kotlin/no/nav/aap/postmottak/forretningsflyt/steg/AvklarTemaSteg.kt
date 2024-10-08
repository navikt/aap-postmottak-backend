package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.sakogbehandling.behandling.DokumentbehandlingRepository
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class AvklarTemaSteg(
    private val dokumentbehandlingRepository: DokumentbehandlingRepository,
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return AvklarTemaSteg(DokumentbehandlingRepository(connection))
        }

        override fun type(): StegType {
            return StegType.AVKLAR_TEMA
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandling = dokumentbehandlingRepository.hentMedLås(kontekst.behandlingId, null)

        return if (!behandling.kanBehandlesAutomatisk() && !behandling.harTemaBlittAvklart()) {
            StegResultat(listOf(Definisjon.AVKLAR_TEMA))
        } else StegResultat()
    }


}