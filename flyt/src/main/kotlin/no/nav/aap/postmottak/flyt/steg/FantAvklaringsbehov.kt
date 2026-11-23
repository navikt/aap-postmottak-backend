package no.nav.aap.postmottak.flyt.steg

import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class FantAvklaringsbehov(private val avklaringsbehov: Definisjon) : StegResultat {

    init {
        require(!avklaringsbehov.erVentebehov()) { "Forventer et manuelt avklaringsbehov, fikk ventebehov: $avklaringsbehov" }
    }

    override fun transisjon(): Transisjon {
        return FunnetAvklaringsbehov(avklaringsbehov)
    }
}