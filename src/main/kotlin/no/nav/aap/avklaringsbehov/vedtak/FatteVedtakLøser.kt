package no.nav.aap.avklaringsbehov.vedtak

import no.nav.aap.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.kontroll.FlytKontekst

class FatteVedtakLøser : AvklaringsbehovsLøser<FatteVedtakLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: FatteVedtakLøsning) {
        // DO NOTHING 4 Now
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FATTE_VEDTAK
    }
}
