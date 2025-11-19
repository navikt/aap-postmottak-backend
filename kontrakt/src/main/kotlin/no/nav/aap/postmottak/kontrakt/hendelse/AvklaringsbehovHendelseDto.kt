package no.nav.aap.postmottak.kontrakt.hendelse

import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status
import java.time.LocalDate
import java.time.LocalDateTime

public data class AvklaringsbehovHendelseDto(
    val avklaringsbehovDefinisjon: Definisjon,
    val status: Status,
    val endringer: List<EndringDTO>
)

public data class EndringDTO(
    val status: Status,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val frist: LocalDate? = null,
    val årsakTilSattPåVent: ÅrsakTilSettPåVent? = null,
    val endretAv: String,
    val begrunnelse: String? = null
)

public enum class ÅrsakTilSettPåVent {
    VENTER_PÅ_OPPLYSNINGER,
    VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER,
    VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
    VENTER_PÅ_VURDERING_AV_ROL,
    VENTER_PÅ_SVAR_FRA_BRUKER,
    VENTER_PÅ_BEHANDLING_I_GOSYS
}
