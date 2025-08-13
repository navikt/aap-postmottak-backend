package no.nav.aap.postmottak.flyt.steg

interface StegKonstruktør {
    fun konstruer(steg: FlytSteg): BehandlingSteg
}