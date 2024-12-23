package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.søknad.parseDigitalSøknad
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.søknad.serialiser
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.DigitaliserDokumentLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository

class DigitaliserDokumentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<DigitaliserDokumentLøsning> {
    val repositoryProvider = RepositoryProvider(connection)
    val struktureringsvurderingRepository = repositoryProvider.provide(StruktureringsvurderingRepository::class)
    val kategorivurderingRepository = repositoryProvider.provide(KategoriVurderingRepository::class)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: DigitaliserDokumentLøsning): LøsningsResultat {

        val kategori = kategorivurderingRepository.hentKategoriAvklaring(kontekst.kontekst.behandlingId)?.avklaring
        requireNotNull(kategori) { "Mangler kategori for digitalisert dokument" }
        requireNotNull(løsning.strukturertDokument) { "Digitalisert dokument kan ikke være null" }

        val dokument = when (kategori) {
            InnsendingType.SØKNAD -> løsning.strukturertDokument.parseDigitalSøknad().serialiser()
            else -> løsning.strukturertDokument
        }

        struktureringsvurderingRepository.lagreStrukturertDokument(kontekst.kontekst.behandlingId, dokument)

        return LøsningsResultat("Dokument er strukturet")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.DIGITALISER_DOKUMENT
    }
}
