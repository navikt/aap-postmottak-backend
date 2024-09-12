package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SECURE_LOGGER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.saf.graphql.SafGraphqlClient
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(AvklarTemaSteg::class.java)

class AvklarTemaSteg(
    private val behandlingRepository: BehandlingRepository,
    private val safGraphQlClient: SafGraphqlClient
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return AvklarTemaSteg(BehandlingRepositoryImpl(connection), SafGraphqlClient.withClientCredentialsRestClient())
        }

        override fun type(): StegType {
            return StegType.AVKLAR_TEMA
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        log.info("Treffer avklartema utfører")
        /* TODO finn avklaring om dokument faktisk skal til AAP eller skal returneres
        *  Hvis dokument er avklart med ja: Stegresultat()
        *  Hvis avklart med nei: Returner avklaringsbehov for returnering av dokument
        *  Hvis ikke avklart enda: returner Definisjon.AVKLAR_TEMA
        */
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val journalpost = safGraphQlClient.hentJournalpost(behandling.journalpostId)

        SECURE_LOGGER.info(behandling.toString())
        SECURE_LOGGER.info(journalpost.toString())

        return if (!journalpost.kanBehandlesAutomatisk() && !behandling.harTemaBlittAvklart()) {
            StegResultat(listOf(Definisjon.AVKLAR_TEMA))
        } else StegResultat()
    }


}