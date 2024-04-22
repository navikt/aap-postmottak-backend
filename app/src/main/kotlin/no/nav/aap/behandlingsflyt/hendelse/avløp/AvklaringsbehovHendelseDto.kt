package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.Status
import no.nav.aap.verdityper.flyt.StegType
import java.time.LocalDate
import java.time.LocalDateTime

data class AvklaringsbehovHendelseDto(
    val definisjon: DefinisjonDTO,
    val status: Status,
    val endringer: List<EndringDTO>
)

data class DefinisjonDTO(
    val type: String,
    val behovType: Definisjon.BehovType,
    val løsesISteg: StegType
)

data class EndringDTO(
    val status: Status,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val frist: LocalDate? = null,
    val endretAv: String
)