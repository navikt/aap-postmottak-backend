package no.nav.aap.postmottak.behandling.avklaringsbehov.løser

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.behandling.avklaringsbehov.løsning.DigitaliserDokumentLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategorivurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.søknad.parseDigitalSøknad
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.søknad.serialiser
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType

class DigitaliserDokumentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<DigitaliserDokumentLøsning> {
    val struktureringsvurderingRepository = StruktureringsvurderingRepository(connection)
    val kategorivurderingRepository = KategorivurderingRepository(connection)

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
