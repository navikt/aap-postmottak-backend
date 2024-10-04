package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.faktagrunnlag.register.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.postmottak.faktagrunnlag.register.behandlingsflyt.BehandlingsflytGateway
import no.nav.aap.postmottak.sakogbehandling.behandling.DokumentbehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.Saksvurdering
import no.nav.aap.postmottak.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.sakogbehandling.Ident


class AvklarSakSteg(
    private val dokumentbehandlingRepository: DokumentbehandlingRepository,
    private val avklaringRepository: AvklaringRepository,
    private val saksnummerRepository: SaksnummerRepository,
    private val journalpostRepository: JournalpostRepository,
    private val behandlingsflytClient: BehandlingsflytGateway
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return AvklarSakSteg(
                DokumentbehandlingRepository(connection),
                AvklaringRepositoryImpl(connection),
                SaksnummerRepository(connection),
                JournalpostRepositoryImpl(connection),
                BehandlingsflytClient()
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_SAK
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val sakerPåBruker = saksnummerRepository.hentSaksnummre(kontekst.behandlingId)
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        val behandling = dokumentbehandlingRepository.hentMedLås(kontekst.behandlingId, null)
        requireNotNull(journalpost) { "Journalpost kan ikke være null" }
        check(journalpost is Journalpost.MedIdent)

        return if (journalpost.kanBehandlesAutomatisk() || sakerPåBruker.isEmpty()) {
            val saksnummer = behandlingsflytClient.finnEllerOpprettSak(Ident(journalpost.personident.id), journalpost.mottattDato()).saksnummer
            avklaringRepository.lagreSakVurdering(kontekst.behandlingId, Saksvurdering(saksnummer, false, false))
            StegResultat()
        } else if (behandling.harGjortSaksvurdering()) {
            if (behandling.vurderinger.saksvurdering?.opprettNySak == true) {
                val saksnummer = behandlingsflytClient.finnEllerOpprettSak(Ident(journalpost.personident.id), journalpost.mottattDato()).saksnummer
                avklaringRepository.lagreSakVurdering(kontekst.behandlingId, Saksvurdering(saksnummer, false, false))
            }
            StegResultat()
        } else {
            return StegResultat(
                listOf(
                    Definisjon.AVKLAR_SAK
                )
            )
        }
    }
}