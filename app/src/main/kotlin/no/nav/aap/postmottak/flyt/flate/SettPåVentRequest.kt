package no.nav.aap.postmottak.flyt.flate

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.postmottak.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandlingsreferanse
import java.time.LocalDate

data class SettPåVentRequest(
    val referanse: Behandlingsreferanse,
    @JsonProperty(
        value = "behandlingVersjon",
        required = true,
        defaultValue = "0"
    ) val behandlingVersjon: Long, val begrunnelse: String, val grunn: ÅrsakTilSettPåVent, val frist: LocalDate?
)