package no.nav.aap.postmottak.flyt.flate

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandlingsreferanse
import java.time.LocalDateTime

data class DetaljertBehandlingDTO(
    val referanse: Behandlingsreferanse,
    val type: String,
    val status: Status,
    val opprettet: LocalDateTime,
    val avklaringsbehov: List<AvklaringsbehovDTO>,
    val aktivtSteg: StegType,
    val versjon: Long
)
