package no.nav.aap.postmottak.flyt.flate

import no.nav.aap.postmottak.behandling.avklaringsbehov.Status
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import java.time.LocalDateTime

data class AvklaringsbehovDTO(
    val definisjon: Definisjon,
    val status: Status,
    val endringer: List<EndringDTO>
)

data class EndringDTO(
    val status: Status,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val begrunnelse: String?,
    val endretAv: String
)
