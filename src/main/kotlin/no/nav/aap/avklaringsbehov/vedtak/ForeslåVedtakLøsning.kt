package no.nav.aap.avklaringsbehov.vedtak

import no.nav.aap.avklaringsbehov.AvklaringsbehovLøsning
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon

class ForeslåVedtakLøsning(begrunnelse: String, endretAv: String) :
    AvklaringsbehovLøsning(Definisjon.FORESLÅ_VEDTAK, begrunnelse, endretAv) {

}
