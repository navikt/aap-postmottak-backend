package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.Status
import java.time.LocalDateTime
import java.util.*

data class DetaljertBehandlingDTO(
    val referanse: JournalpostId,
    val type: String,
    val status: Status,
    val opprettet: LocalDateTime,
    val avklaringsbehov: List<AvklaringsbehovDTO>,
    val aktivtSteg: StegType,
    val versjon: Long
)
