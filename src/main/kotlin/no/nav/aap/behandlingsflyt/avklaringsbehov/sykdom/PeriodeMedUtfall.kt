package no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.behandlingsflyt.domene.behandling.Utfall
import no.nav.aap.behandlingsflyt.domene.Periode

data class PeriodeMedUtfall(
    @JsonProperty("periode") val periode: Periode,
    @JsonProperty("utfall") val utfall: Utfall,
    @JsonProperty("begrunnelse") val begrunnelse: String
)