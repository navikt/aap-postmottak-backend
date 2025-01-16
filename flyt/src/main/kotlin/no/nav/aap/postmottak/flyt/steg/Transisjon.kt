package no.nav.aap.postmottak.flyt.steg

import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

sealed interface Transisjon {
    fun kanFortsette(): Boolean = true
}

object Fortsett : Transisjon
object Stopp : Transisjon {
    override fun kanFortsette(): Boolean {
        return false
    }
}

class FunnetAvklaringsbehov(private val avklaringsbehov: List<Definisjon>) : Transisjon {
    fun avklaringsbehov(): List<Definisjon> {
        return avklaringsbehov
    }
}

class FunnetVentebehov(private val ventebehov: List<Ventebehov>) : Transisjon {
    fun ventebehov(): List<Ventebehov> {
        return ventebehov
    }
}