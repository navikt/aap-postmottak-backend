package no.nav.aap.behandlingsflyt.avklaringsbehov.arbeidsevne

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FASTSETT_ARBEIDSEVNE_KODE

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FASTSETT_ARBEIDSEVNE_KODE)
class FastsettArbeidsevneLøsning(
) :
    AvklaringsbehovLøsning
