package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import mottak.saf.SafGraphqlClient
import mottak.saf.SafGraphqlGateway
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.saf.Journalpost
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Private

class DigitaliserDokumentSteg(
    private val behandlingRepository: BehandlingRepository,
    private val safGraphqlGateway: SafGraphqlGateway
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return DigitaliserDokumentSteg(BehandlingRepositoryImpl(connection), SafGraphqlClient)
        }

        override fun type(): StegType {
            return StegType.DIGITALISER_DOKUMENT
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val journalpost = safGraphqlGateway.hentJournalpost(behandling.journalpostId)

        require(journalpost is Journalpost.MedIdent)

        return if (!journalpost.kanBehandlesAutomatisk() && !behandling.harBlittStrukturert()) StegResultat(
            listOf(
                Definisjon.DIGITALISER_DOKUMENT
            )
        )
        else StegResultat()
    }
}