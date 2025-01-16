package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.Saksvurdering
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType

class AvklarSakSteg(
    private val saksnummerRepository: SaksnummerRepository,
    private val journalpostRepository: JournalpostRepository,
    private val behandlingsflytClient: BehandlingsflytGateway
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            return AvklarSakSteg(
                repositoryProvider.provide(SaksnummerRepository::class),
                repositoryProvider.provide(JournalpostRepository::class),
                GatewayProvider.provide(BehandlingsflytGateway::class)
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_SAK
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val journalpost =
            journalpostRepository.hentHvisEksisterer(kontekst.behandlingId) ?: error("Journalpost kan ikke være null")

        if (journalpost?.tema != "AAP") {
            return Fullført
        }

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