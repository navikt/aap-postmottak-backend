package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurderingRepository
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType

class KategoriserDokumentSteg(
    private val kategorivurderingRepository: KategoriVurderingRepository,
    private val journalpostRepository: JournalpostRepository
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            return KategoriserDokumentSteg(
                repositoryProvider.provide(KategoriVurderingRepository::class),
                repositoryProvider.provide(JournalpostRepository::class)
            )
        }

        override fun type(): StegType {
            return StegType.KATEGORISER_DOKUMENT
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        requireNotNull(journalpost)

        if (journalpost.erDigitalSøknad()) {
            kategorivurderingRepository.lagreKategoriseringVurdering(kontekst.behandlingId, InnsendingType.SØKNAD)
            return Fullført
        } else if (journalpost.erDigitalLegeerklæring()) {
            kategorivurderingRepository.lagreKategoriseringVurdering(
                kontekst.behandlingId,
                InnsendingType.LEGEERKLÆRING
            )
            return Fullført
        }

        val kategorivurdering = kategorivurderingRepository.hentKategoriAvklaring(kontekst.behandlingId)
        return if (kategorivurdering == null)
            FantAvklaringsbehov(
                Definisjon.KATEGORISER_DOKUMENT
            )
        else Fullført
    }
}