package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.DigitaliserDokumentLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KategoriserDokumentLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.flate.DigitaliserDokumentDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl

class DigitaliserDokumentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<DigitaliserDokumentLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: DigitaliserDokumentLøsning): LøsningsResultat {

        // TODO valider strukturert dokument
        BehandlingRepositoryImpl(connection)
            .lagreStrukturertDokument(kontekst.kontekst.behandlingId, løsning.strukturertDokument.json)

        return LøsningsResultat("Dokument er strukturet")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.DIGITALISER_DOKUMENT
    }
}
