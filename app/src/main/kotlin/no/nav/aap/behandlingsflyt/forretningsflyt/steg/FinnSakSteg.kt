package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt.BehandlingsflytGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.behandlingsflyt.forretningsflyt.informasjonskrav.saksnummer.SaksnummerRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.sakogbehandling.Ident


class FinnSakSteg(
    private val behandlingRepository: BehandlingRepositoryImpl,
    private val saksnummerRepository: SaksnummerRepository,
    private val journalpostRepository: JournalpostRepository,
    private val behandlingsflytClient: BehandlingsflytGateway
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return FinnSakSteg(
                BehandlingRepositoryImpl(connection),
                SaksnummerRepository(connection),
                JournalpostRepositoryImpl(connection),
                BehandlingsflytClient()
            )
        }

        override fun type(): StegType {
            return StegType.FINN_SAK
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val sakerPåBruker = saksnummerRepository.hentSaksnummre(kontekst.behandlingId)
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        requireNotNull(journalpost) { "Journalpost kna ikke være null" }
        check(journalpost is Journalpost.MedIdent)

        return if (journalpost.kanBehandlesAutomatisk() || sakerPåBruker.isEmpty()) {
            val saksnummer = behandlingsflytClient.finnEllerOpprettSak(Ident(journalpost.personident.id), journalpost.mottattDato()).saksnummer
            behandlingRepository.lagreSakVurdeirng(kontekst.behandlingId, Saksnummer(saksnummer))
            StegResultat()
        } else if (behandling.harGjortSaksvurdering()) {
            if (behandling.vurderinger.saksvurdering == null) {
                val saksnummer = behandlingsflytClient.finnEllerOpprettSak(Ident(journalpost.personident.id), journalpost.mottattDato()).saksnummer
                behandlingRepository.lagreSakVurdeirng(kontekst.behandlingId, Saksnummer(saksnummer))
            }
            StegResultat()
        } else {
            return StegResultat(
                listOf(
                    Definisjon.AVKLAR_SAKSNUMMER
                )
            )
        }
    }
}