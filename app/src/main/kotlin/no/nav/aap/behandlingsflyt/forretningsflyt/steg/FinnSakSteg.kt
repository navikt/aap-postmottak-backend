package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt.BehandlingsflytGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.behandlingsflyt.saf.graphql.SafGraphqlClient
import no.nav.aap.behandlingsflyt.saf.graphql.SafGraphqlGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.Ident


class FinnSakSteg(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingsflytClient: BehandlingsflytGateway,
    private val safGraphQlClient: SafGraphqlGateway
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return FinnSakSteg(
                BehandlingRepositoryImpl(connection),
                BehandlingsflytClient(),
                SafGraphqlClient.withClientCredentialsRestClient()
            )
        }

        override fun type(): StegType {
            return StegType.FINN_SAK
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val journalpost = safGraphQlClient.hentJournalpost(behandling.journalpostId)
        require(journalpost is Journalpost.MedIdent)

        if (journalpost.kanBehandlesAutomatisk()) {
            val saksnummer = behandlingsflytClient
                .finnEllerOpprettSak(Ident(journalpost.personident.id), journalpost.mottattDato())

            behandlingRepository.lagreSaksnummer(behandling.id, saksnummer.saksnummer)
        } else {
            val saksnumre = behandlingsflytClient.finnSaker(Ident(journalpost.personident.id))
            if (saksnumre.size > 1) {
                return StegResultat(
                    listOf(
                        Definisjon.AVKLAR_SAKSNUMMER
                    )
                )
            } else if (saksnumre.isEmpty()) {
                val saksnummer = behandlingsflytClient
                    .finnEllerOpprettSak(Ident(journalpost.personident.id), journalpost.mottattDato())

                behandlingRepository.lagreSaksnummer(behandling.id, saksnummer.saksnummer)
            } else {
                behandlingRepository.lagreSaksnummer(behandling.id, saksnumre.first().saksnummer)
            }
        }
            
        return StegResultat()
    }
}