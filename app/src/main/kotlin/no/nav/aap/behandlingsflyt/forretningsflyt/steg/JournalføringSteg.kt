package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.joark.Joark
import no.nav.aap.behandlingsflyt.joark.JoarkClient
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.httpklient.httpclient.error.UhåndtertHttpResponsException
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory


class JournalføringSteg(
    private val behandlingRepository: BehandlingRepository,
    private val journalpostRepository: JournalpostRepository,
    private val joarkKlient: Joark
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(JournalføringSteg::class.java)

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return JournalføringSteg(
                BehandlingRepositoryImpl(connection),
                JournalpostRepositoryImpl(connection),
                JoarkClient()
            )
        }

        override fun type(): StegType {
            return StegType.ENDERLIG_JOURNALFØRING
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)

        require(journalpost is Journalpost.MedIdent)

        joarkKlient.oppdaterJournalpost(journalpost, behandling.saksnummer.toString())

        try {
            joarkKlient.ferdigstillJournalpost(journalpost)
        } catch (e: UhåndtertHttpResponsException) {
            log.warn("Kunne ikke ferdigstille ${journalpost.journalpostId}: ${e.message}")
            throw e
        }

        return StegResultat()
    }
}