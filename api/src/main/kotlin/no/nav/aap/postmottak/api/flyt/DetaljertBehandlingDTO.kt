package no.nav.aap.postmottak.api.flyt

import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import java.time.LocalDateTime

data class DetaljertBehandlingDTO(
    val referanse: BehandlingsreferansePathParam,
    val type: String,
    val status: Status,
    val opprettet: LocalDateTime,
    val avklaringsbehov: List<AvklaringsbehovDTO>,
    val aktivtSteg: StegType,
    val versjon: Long
)
