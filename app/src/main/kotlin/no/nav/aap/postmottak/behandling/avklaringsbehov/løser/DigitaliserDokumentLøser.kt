package no.nav.aap.postmottak.behandling.avklaringsbehov.løser

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.behandling.avklaringsbehov.løsning.DigitaliserDokumentLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategorivurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.søknad.parseDigitalSøknad
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.søknad.serialiser
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl

class DigitaliserDokumentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<DigitaliserDokumentLøsning> {
    val avklaringRepository = AvklaringRepositoryImpl(connection)
    val kategorivurderingRepository = KategorivurderingRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: DigitaliserDokumentLøsning): LøsningsResultat {

        val brevkode = kategorivurderingRepository.hentKategoriAvklaring(kontekst.kontekst.behandlingId)?.avklaring
        requireNotNull(brevkode) { "Mangler kategori for digitalisert dokument" }
        requireNotNull(løsning.strukturertDokument) { "Digitalisert dokument kan ikke være null" }

        val dokument = when (brevkode) {
            Brevkode.SØKNAD -> løsning.strukturertDokument.parseDigitalSøknad().serialiser()
            else -> løsning.strukturertDokument!!
        }

        avklaringRepository.lagreStrukturertDokument(kontekst.kontekst.behandlingId, dokument)

        return LøsningsResultat("Dokument er strukturet")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.DIGITALISER_DOKUMENT
    }
}
