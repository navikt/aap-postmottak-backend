package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.joark.Joark
import no.nav.aap.behandlingsflyt.joark.JoarkClient
import no.nav.aap.behandlingsflyt.saf.Journalpost
import no.nav.aap.behandlingsflyt.saf.graphql.SafGraphqlClient
import no.nav.aap.behandlingsflyt.saf.graphql.SafGraphqlGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(JournalføringSteg::class.java)

class JournalføringSteg(
    private val behandlingRepository: BehandlingRepository,
    private val safGraphqlGateway: SafGraphqlGateway,
    private val joarkKlient: Joark
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return JournalføringSteg(
                BehandlingRepositoryImpl(connection),
                SafGraphqlClient.withClientCredentialsRestClient(),
                JoarkClient()
            )
        }

        override fun type(): StegType {
            return StegType.ENDERLIG_JOURNALFØRING
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        log.info("Treffer JournalføringsstegSteg")

        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val journalpost = safGraphqlGateway.hentJournalpost(behandling.journalpostId)

        require(journalpost is Journalpost.MedIdent)

        joarkKlient.oppdaterJournalpost(journalpost, behandling.saksnummer.toString())
        joarkKlient.ferdigstillJournalpost(journalpost)

        return StegResultat()
    }
}