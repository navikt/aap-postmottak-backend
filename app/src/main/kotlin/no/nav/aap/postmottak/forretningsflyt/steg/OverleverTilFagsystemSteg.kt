package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.SafRestClient
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.overlevering.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.postmottak.overlevering.behandlingsflyt.BehandlingsflytGateway
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(OverleverTilFagsystemSteg::class.java)

class OverleverTilFagsystemSteg(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingsflytGateway: BehandlingsflytGateway,
    private val journalpostRepository: JournalpostRepository,
    private val safRestClient: SafRestClient
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return OverleverTilFagsystemSteg(
                BehandlingRepositoryImpl(connection),
                BehandlingsflytClient(),
                JournalpostRepositoryImpl(connection),
                SafRestClient.withClientCredentialsRestClient()
            )
        }

        override fun type(): StegType {
            return StegType.OVERLEVER_TIL_FAGSYSTEM
        }
    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        require(journalpost != null)

        // TODO :poop: bør kanskje gjøres på journalpost
        val dokumentJson = if (behandling.harBlittStrukturert()) behandling.vurderinger.struktureringsvurdering!!.vurdering.toByteArray()
            else hentDokumentFraSaf(journalpost)

        behandlingsflytGateway.sendSøknad(behandling.vurderinger.saksvurdering?.vurdering?.saksnummer!!, journalpost.journalpostId ,dokumentJson)

        return StegResultat()
    }

    private fun hentDokumentFraSaf(journalpost: Journalpost): ByteArray {
        val strukturertDokument = journalpost.finnOriginal()
        requireNotNull(strukturertDokument) { "Finner ikke strukturert dokument" }
        return safRestClient.hentDokument(journalpost.journalpostId, strukturertDokument.dokumentInfoId).dokument.readBytes()
    }
}