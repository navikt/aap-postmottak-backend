package no.nav.aap.flate.sak

import no.nav.aap.domene.behandling.Status
import java.time.LocalDateTime
import java.util.*

data class BehandlinginfoDTO(
    val referanse: UUID,
    val type: String,
    val status: Status,
    val opprettet: LocalDateTime
) {

}
