package no.nav.aap.avklaringsbehov.vedtak

import no.nav.aap.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.kontroll.FlytKontekst

class ForeslåVedtakLøser : AvklaringsbehovsLøser<ForeslåVedtakLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: ForeslåVedtakLøsning) {
        // DO NOTHING 4 Now
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FORESLÅ_VEDTAK
    }
}
