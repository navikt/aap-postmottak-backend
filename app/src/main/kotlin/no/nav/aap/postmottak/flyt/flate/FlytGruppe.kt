package no.nav.aap.postmottak.flyt.flate

import no.nav.aap.postmottak.kontrakt.steg.StegGruppe

data class FlytGruppe(
    val stegGruppe: StegGruppe,
    val erFullført: Boolean,
    val steg: List<FlytSteg>,
    val skalVises: Boolean
)
