package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.DigitaliserDokumentLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.GrovkategoriserDokumentLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KategoriserDokumentLøsning
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.flate.DigitaliserDokumentDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl

class GrovkategoriserDokumentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<GrovkategoriserDokumentLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: GrovkategoriserDokumentLøsning): LøsningsResultat {

        BehandlingRepositoryImpl(connection).lagreGrovvurdeingVurdering(kontekst.kontekst.behandlingId, løsning.skalTilAap)

        return LøsningsResultat("Dokumnent er ${if (løsning.skalTilAap) "" else "ikke"} ment for AAP")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.GROVKATEGORISER_DOKUMENT
    }
}
