package no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.behandlingsflyt.flyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.Periode

data class PeriodeMedUtfall(
    @JsonProperty("periode") val periode: Periode,
    @JsonProperty("utfall") val utfall: Utfall,
    @JsonProperty("begrunnelse") val begrunnelse: String
)