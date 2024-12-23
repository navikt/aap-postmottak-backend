package no.nav.aap.postmottak.flyt.steg

import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class FantAvklaringsbehov(private val avklaringsbehov: List<Definisjon>) : StegResultat {
    constructor(definisjon: Definisjon) : this(listOf(definisjon))

    init {
        require(avklaringsbehov.isNotEmpty())
        require(avklaringsbehov.none { it.erVentebehov() }) { "Inneholder ventebehov, disse bruk Stegresultat FantVentebehov:: $avklaringsbehov" }
    }

    override fun transisjon(): Transisjon {
        return FunnetAvklaringsbehov(avklaringsbehov)
    }
}