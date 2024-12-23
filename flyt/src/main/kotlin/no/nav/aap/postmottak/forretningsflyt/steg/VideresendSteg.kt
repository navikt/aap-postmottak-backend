package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører

class VideresendSteg(
    val saksnummerRepository: SaksnummerRepository,
    val avklarTemaRepository: AvklarTemaRepository,
    val behandlingRepository: BehandlingRepository,
    val flytJobbRepository: FlytJobbRepository,
    val kopierer: GrunnlagKopierer
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            return VideresendSteg(
                repositoryProvider.provide(SaksnummerRepository::class),
                repositoryProvider.provide(AvklarTemaRepository::class),
                repositoryProvider.provide(BehandlingRepository::class),
                FlytJobbRepository(connection),
                GrunnlagKopierer(connection)
            )
        }

        override fun type(): StegType {
            return StegType.VIDERESEND
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val saksnummervurdering = saksnummerRepository.hentSakVurdering(kontekst.behandlingId)
        val avklarTemavurdering= avklarTemaRepository.hentTemaAvklaring(kontekst.behandlingId)
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        if (saksnummervurdering?.generellSak == true || avklarTemavurdering?.skalTilAap == false) {
            return Fullført
        }

        val dokumentbehandlingId = behandlingRepository.opprettBehandling(behandling.journalpostId, TypeBehandling.DokumentHåndtering)
        kopierer.overfør(kontekst.behandlingId, dokumentbehandlingId)
        flytJobbRepository.leggTil(
            JobbInput(ProsesserBehandlingJobbUtfører)
                .forBehandling(behandling.journalpostId.referanse, dokumentbehandlingId.id).medCallId()
        )

            return Fullført
    }
}
