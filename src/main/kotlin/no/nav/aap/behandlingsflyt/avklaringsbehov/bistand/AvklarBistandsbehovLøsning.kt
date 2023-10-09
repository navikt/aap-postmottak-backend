package no.nav.aap.behandlingsflyt.avklaringsbehov.bistand

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.AVKLAR_BISTANDSBEHOV_KODE

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_BISTANDSBEHOV_KODE)
class AvklarBistandsbehovLøsning(
    @JsonProperty("bistandsVurdering", required = true) val bistandsVurdering: BistandsVurdering
) :
    AvklaringsbehovLøsning
