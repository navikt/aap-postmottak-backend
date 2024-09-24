package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.behandlingsflyt.flyt.flate.visning.Prosessering
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe
import no.nav.aap.postmottak.kontrakt.steg.StegType

data class BehandlingFlytOgTilstandDto(
    val flyt: List<FlytGruppe>,
    val aktivtSteg: StegType,
    val aktivGruppe: StegGruppe,
    val behandlingVersjon: Long,
    val prosessering: Prosessering
)
