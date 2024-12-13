package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytKlient
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.Saksvurdering
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.sakogbehandling.Ident


class AvklarSakSteg(
    private val saksnummerRepository: SaksnummerRepository,
    private val journalpostRepository: JournalpostRepository,
    private val behandlingsflytClient: BehandlingsflytKlient
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return AvklarSakSteg(
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
        val journalpost =
            journalpostRepository.hentHvisEksisterer(kontekst.behandlingId) ?: error("Journalpost kan ikke være null")
        val saksnummerVurdering = saksnummerRepository.hentSakVurdering(kontekst.behandlingId)
        requireNotNull(journalpost)

        return if (journalpost.erDigitalSøknad()) {
            val saksnummer = behandlingsflytClient.finnEllerOpprettSak(
                Ident(journalpost.person.aktivIdent().identifikator),
                journalpost.mottattDato()
            ).saksnummer
            saksnummerRepository.lagreSakVurdering(kontekst.behandlingId, Saksvurdering(saksnummer, false, false))
            Fullført
        } else if (saksnummerVurdering != null) {
            if (saksnummerVurdering.opprettNySak) {
                val saksnummer = behandlingsflytClient.finnEllerOpprettSak(
                    Ident(journalpost.person.aktivIdent().identifikator),
                    journalpost.mottattDato()
                ).saksnummer
                saksnummerRepository.lagreSakVurdering(kontekst.behandlingId, Saksvurdering(saksnummer, false, false))
            }
            Fullført
        } else {
            return FantAvklaringsbehov(
                Definisjon.AVKLAR_SAK
            )
        }
    }
}