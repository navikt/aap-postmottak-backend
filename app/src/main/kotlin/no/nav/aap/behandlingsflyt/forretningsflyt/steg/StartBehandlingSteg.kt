package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import mottak.saf.SafGraphqlClient
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.SafRestClient
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.saf.Journalpost
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(StartBehandlingSteg::class.java)

class StartBehandlingSteg private constructor(
    private var behandlingRepository: BehandlingRepository,
    private val safGraphQlClient: SafGraphqlClient) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return StartBehandlingSteg(BehandlingRepositoryImpl(connection), SafGraphqlClient)
        }

        override fun type(): StegType {
            return StegType.START_BEHANDLING
        }
    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        log.info("Treffer Start behandling steg")
        /* TODO forsøk å automatisk utrede dokument type
        *  Hvis vi automatisk kan si at brevet skal til AAP til FinnSak
        *  Hvis ikke send til Avklar tema
        */
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val journalpost = safGraphQlClient.hentJournalpost(behandling.journalpostId)

        return if (måBehandlesManuelt(journalpost)) StegResultat(listOf(Definisjon.AVKLAR_TEMA))
            else StegResultat()
    }

    private fun måBehandlesManuelt(journalpost: Journalpost): Boolean {
        return !(journalpost.erSøknad() && journalpost.erDigital())
    }

}
