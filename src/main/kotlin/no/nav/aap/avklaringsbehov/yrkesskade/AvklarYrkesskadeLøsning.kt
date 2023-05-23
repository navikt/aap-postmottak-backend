package no.nav.aap.avklaringsbehov.yrkesskade

import no.nav.aap.avklaringsbehov.AvklaringsbehovLøsning
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon

class AvklarYrkesskadeLøsning(begrunnelse: String, endretAv: String) :
    AvklaringsbehovLøsning(Definisjon.AVKLAR_YRKESSKADE, begrunnelse, endretAv) {

}
