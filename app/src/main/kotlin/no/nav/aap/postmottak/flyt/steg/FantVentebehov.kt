package no.nav.aap.postmottak.flyt.steg

class FantVentebehov(private val ventebehov: List<Ventebehov>) : StegResultat {
    constructor(ventebehov: Ventebehov) : this(listOf(ventebehov))

    init {
        require(ventebehov.isNotEmpty())
        require(ventebehov.all { it.definisjon.erVentebehov() }) { "Inneholder avklaringsbehov bruk Stegresultat FantAvklaringsbehov for disse :: $ventebehov" }
    }

    override fun transisjon(): Transisjon {
        return FunnetVentebehov(ventebehov)
    }
}