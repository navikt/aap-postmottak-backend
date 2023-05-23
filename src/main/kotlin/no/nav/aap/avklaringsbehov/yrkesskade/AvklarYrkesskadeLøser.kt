package no.nav.aap.avklaringsbehov.yrkesskade

import no.nav.aap.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.kontroll.FlytKontekst

class AvklarYrkesskadeLøser : AvklaringsbehovsLøser<AvklarYrkesskadeLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: AvklarYrkesskadeLøsning) {
        // DO NOTHING 4 Now
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_YRKESSKADE
    }
}
