package no.nav.aap.postmottak.kontrakt.hendelse

import no.nav.aap.postmottak.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status
import no.nav.aap.postmottak.kontrakt.steg.StegType
import java.time.LocalDate
import java.time.LocalDateTime

data class AvklaringsbehovHendelseDto(
    @Deprecated(message = "Bruk direkte definisjon.")
    val definisjon: DefinisjonDTO?,
    val avklaringsbehovDefinisjon: Definisjon,
    val status: Status,
    val endringer: List<EndringDTO>
)

data class DefinisjonDTO(
    val type: AvklaringsbehovKode,
    val behovType: Definisjon.BehovType,
    val løsesISteg: StegType
)

data class EndringDTO(
    val status: Status,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val frist: LocalDate? = null,
    val årsakTilSattPåVent: ÅrsakTilSettPåVent? = null,
    val endretAv: String
)

enum class ÅrsakTilSettPåVent {
    VENTER_PÅ_OPPLYSNINGER,
    VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER,
    VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
    VENTER_PÅ_VURDERING_AV_ROL,
    VENTER_PÅ_SVAR_FRA_BRUKER
}
