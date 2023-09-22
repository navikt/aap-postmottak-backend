package no.nav.aap.avklaringsbehov.sykdom

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.avklaringsbehov.AvklaringsbehovLøsning
import no.nav.aap.domene.behandling.avklaringsbehov.AVKLAR_SYKDOM_KODE

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_SYKDOM_KODE)
class AvklarSykdomLøsning(
    @JsonProperty("yrkesskadevurdering", required = true) val yrkesskadevurdering: Yrkesskadevurdering?,
    @JsonProperty("sykdomsvurdering", required = true) val sykdomsvurdering: Sykdomsvurdering
) :
    AvklaringsbehovLøsning {

}
