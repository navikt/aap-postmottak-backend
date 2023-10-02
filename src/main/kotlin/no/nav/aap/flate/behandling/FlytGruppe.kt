package no.nav.aap.flate.behandling

import no.nav.aap.flyt.StegGruppe

data class FlytGruppe(val stegGruppe: StegGruppe, val steg: List<FlytSteg>) {
}