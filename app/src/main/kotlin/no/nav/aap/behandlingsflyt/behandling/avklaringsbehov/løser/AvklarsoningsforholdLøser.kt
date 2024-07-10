package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSoningsforholdLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

class AvklarsoningsforholdLøser(connection: DBConnection) : AvklaringsbehovsLøser<AvklarSoningsforholdLøsning> {
    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSoningsforholdLøsning): LøsningsResultat {
        // TODO midlertidig implementasjon
        return LøsningsResultat(løsning.soningsvurdering.begrunnelseForSoningUtenforAnstalt)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SONINGSFORRHOLD
    }

}
