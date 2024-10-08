package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.sakogbehandling.behandling.DokumentbehandlingRepository
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class DigitaliserDokumentSteg(
    private val dokumentbehandlingRepository: DokumentbehandlingRepository,
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return DigitaliserDokumentSteg(DokumentbehandlingRepository(connection))
        }

        override fun type(): StegType {
            return StegType.DIGITALISER_DOKUMENT
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandling = dokumentbehandlingRepository.hentMedLås(kontekst.behandlingId)

        return if (!behandling.kanBehandlesAutomatisk() && !behandling.harBlittStrukturert()) StegResultat(
            listOf(
                Definisjon.DIGITALISER_DOKUMENT
            )
        )
        else StegResultat()
    }
}