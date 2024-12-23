package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.KategoriserDokumentLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurderingRepository
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class KategoriserDokumentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<KategoriserDokumentLøsning> {
    private val repositoryProvider = RepositoryProvider(connection)
    private val kategoriVurderingRepository = repositoryProvider.provide(KategoriVurderingRepository::class)
    
    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: KategoriserDokumentLøsning): LøsningsResultat {
        kategoriVurderingRepository.lagreKategoriseringVurdering(kontekst.kontekst.behandlingId, løsning.kategori)
        return LøsningsResultat(løsning.kategori.toString())
    }

    override fun forBehov(): Definisjon {
        return Definisjon.KATEGORISER_DOKUMENT
    }
}
