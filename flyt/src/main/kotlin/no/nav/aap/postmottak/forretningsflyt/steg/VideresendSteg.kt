package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører

class VideresendSteg(
    private val saksnummerRepository: SaksnummerRepository,
    private val avklarTemaRepository: AvklarTemaRepository,
    private val behandlingRepository: BehandlingRepository,
    private val journalpostRepository: JournalpostRepository,
    private val flytJobbRepository: FlytJobbRepository,
    private val kopierer: GrunnlagKopierer
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            return VideresendSteg(
                repositoryProvider.provide(SaksnummerRepository::class),
                repositoryProvider.provide(AvklarTemaRepository::class),
                repositoryProvider.provide(BehandlingRepository::class),
                repositoryProvider.provide(JournalpostRepository::class),
                FlytJobbRepository(connection),
                GrunnlagKopierer(connection)
            )
        }

        override fun type(): StegType {
            return StegType.VIDERESEND
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        requireNotNull(journalpost) { "Journalpost skal eksistere før VideresendSteg" }
        if (journalpost.erUgyldig()) {
            return Fullført
        }
        
        val saksnummervurdering = saksnummerRepository.hentSakVurdering(kontekst.behandlingId)
        requireNotNull(saksnummervurdering) { "Saksnummer skal være avklart før VideresendSteg" }
        val avklarTemaVurdering = avklarTemaRepository.hentTemaAvklaring(kontekst.behandlingId)
        requireNotNull(avklarTemaVurdering) { "Tema skal være avklart før VideresendSteg" }
     
        if (!avklarTemaVurdering.skalTilAap || saksnummervurdering.generellSak) {
            return Fullført
        }

        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        val dokumentbehandlingId =
            behandlingRepository.opprettBehandling(behandling.journalpostId, TypeBehandling.DokumentHåndtering)
        kopierer.overfør(kontekst.behandlingId, dokumentbehandlingId)
        flytJobbRepository.leggTil(
            JobbInput(ProsesserBehandlingJobbUtfører)
                .forBehandling(behandling.journalpostId.referanse, dokumentbehandlingId.id).medCallId()
        )

        return Fullført
    }
}
