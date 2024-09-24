package no.nav.aap.postmottak.sakogbehandling.sak.flate

import no.nav.aap.postmottak.kontrakt.journalpost.Status
import java.time.LocalDateTime
import java.util.*

data class BehandlinginfoDTO(
    val referanse: UUID,
    val type: String,
    val status: Status,
    val opprettet: LocalDateTime
)
