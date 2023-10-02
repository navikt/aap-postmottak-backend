package no.nav.aap.behandlingsflyt.flate.behandling

import no.nav.aap.behandlingsflyt.flyt.StegGruppe
import no.nav.aap.behandlingsflyt.flyt.StegType

class BehandlingFlytOgTilstand2Dto(val flyt: List<FlytGruppe>, val aktivtSteg: StegType, val aktivGruppe: StegGruppe)
