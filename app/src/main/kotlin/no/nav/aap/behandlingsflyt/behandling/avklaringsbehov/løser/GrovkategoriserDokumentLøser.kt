package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.DigitaliserDokumentLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.GrovkategoriserDokumentLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KategoriserDokumentLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.flate.DigitaliserDokumentDto

class GrovkategoriserDokumentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<GrovkategoriserDokumentLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: GrovkategoriserDokumentLøsning): LøsningsResultat {

        TODO("implementer logikk for lagring av løsning")

        return LøsningsResultat("CHANGE ME")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.DIGITALISER_DOKUMENT
    }
}
