package no.nav.aap.postmottak.api.flyt

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import java.time.LocalDate

data class SettPåVentRequest(
    @JsonProperty(
        value = "behandlingVersjon",
        required = true,
        defaultValue = "0"
    ) val behandlingVersjon: Long, val begrunnelse: String, val grunn: ÅrsakTilSettPåVent, val frist: LocalDate?
)