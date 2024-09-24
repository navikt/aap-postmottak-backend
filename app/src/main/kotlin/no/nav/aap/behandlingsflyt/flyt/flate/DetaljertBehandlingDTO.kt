package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.postmottak.kontrakt.journalpost.Status
import no.nav.aap.postmottak.kontrakt.steg.StegType
import java.time.LocalDateTime

data class DetaljertBehandlingDTO(
    val referanse: JournalpostId,
    val type: String,
    val status: Status,
    val opprettet: LocalDateTime,
    val avklaringsbehov: List<AvklaringsbehovDTO>,
    val aktivtSteg: StegType,
    val versjon: Long
)
