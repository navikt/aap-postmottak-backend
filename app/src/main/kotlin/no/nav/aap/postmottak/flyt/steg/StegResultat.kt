package no.nav.aap.postmottak.flyt.steg

sealed interface StegResultat {
    fun transisjon(): Transisjon
}