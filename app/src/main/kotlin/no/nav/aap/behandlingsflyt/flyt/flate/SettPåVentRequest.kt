package no.nav.aap.behandlingsflyt.flyt.flate

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.ÅrsakTilSettPåVent
import java.time.LocalDate

class SettPåVentRequest(
    @JsonProperty(
        value = "behandlingVersjon",
        required = true,
        defaultValue = "0"
    ) val behandlingVersjon: Long, val begrunnelse: String, val grunn: ÅrsakTilSettPåVent, val frist: LocalDate?
)