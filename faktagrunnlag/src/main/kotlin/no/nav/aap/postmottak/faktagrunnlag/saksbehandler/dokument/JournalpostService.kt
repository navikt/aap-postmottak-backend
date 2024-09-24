package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.saf.graphql.SafGraphqlClient
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst
import org.slf4j.LoggerFactory

class JournalpostService private constructor(
    private val journalpostRepository: JournalpostRepository,
    private val safGraphqlClient: SafGraphqlClient,
    private val behandlingRepository: BehandlingRepository
) : Informasjonskrav {
    private val log = LoggerFactory.getLogger(JournalpostService::class.java)


    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): JournalpostService {
            return JournalpostService(
                JournalpostRepositoryImpl(connection),
                SafGraphqlClient.withClientCredentialsRestClient(),
                BehandlingRepositoryImpl(connection)
            )
        }
    }

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val persistertJournalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        
        val journalpostId =
            persistertJournalpost?.journalpostId ?: behandlingRepository.hent(kontekst.behandlingId).journalpostId
        
        val journalpost = safGraphqlClient.hentJournalpost(journalpostId)
        
        if (persistertJournalpost == null) {
            journalpostRepository.lagre(journalpost, kontekst.behandlingId)
            return false
        }
        
        if (persistertJournalpost != journalpost) {
            log.info("Fant endringer i journalposten for behandling ${kontekst.behandlingId}")
            // TODO: Finn ut hvordan man håndterer endringer - gjør ingenting akkurat nå
        }

        return true
    }

}