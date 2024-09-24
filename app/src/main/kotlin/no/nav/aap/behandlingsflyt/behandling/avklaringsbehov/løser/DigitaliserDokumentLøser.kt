package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.DigitaliserDokumentLøsning
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class DigitaliserDokumentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<DigitaliserDokumentLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: DigitaliserDokumentLøsning): LøsningsResultat {

        // TODO valider strukturert dokument
        BehandlingRepositoryImpl(connection)
            .lagreStrukturertDokument(kontekst.kontekst.behandlingId, løsning.strukturertDokument!!)

        return LøsningsResultat("Dokument er strukturet")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.DIGITALISER_DOKUMENT
    }
}
