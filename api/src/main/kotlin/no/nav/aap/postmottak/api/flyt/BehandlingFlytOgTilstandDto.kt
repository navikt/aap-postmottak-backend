package no.nav.aap.postmottak.api.flyt

import no.nav.aap.postmottak.flyt.flate.visning.Visning
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe
import no.nav.aap.postmottak.kontrakt.steg.StegType
import java.util.UUID

data class BehandlingFlytOgTilstandDto(
    val flyt: List<FlytGruppe>,
    val aktivtSteg: StegType,
    val aktivGruppe: StegGruppe,
    val behandlingVersjon: Long,
    val prosessering: Prosessering,
    val visning: Visning,
    val nesteBehandlingId: UUID?
)
