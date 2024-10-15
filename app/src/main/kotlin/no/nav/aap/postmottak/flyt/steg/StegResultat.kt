package no.nav.aap.postmottak.flyt.steg

import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class StegResultat(
    val avklaringsbehov: List<Definisjon> = listOf(),
    val tilbakeførtFraBeslutter: Boolean = false,
    val tilbakeførtFraKvalitetssikrer: Boolean = false,
    val avbrytFlyt: Boolean = false,
) {

    fun transisjon(): Transisjon {
        if (tilbakeførtFraBeslutter) {
            return TilbakeførtFraBeslutter
        }
        if (tilbakeførtFraKvalitetssikrer) {
            return TilbakeførtFraKvalitetssikrer
        }
        if (avklaringsbehov.isNotEmpty()) {
            return FunnetAvklaringsbehov(avklaringsbehov)
        }
        if (avbrytFlyt) return AvbrytEtterAvklaring
        return Fortsett
    }
}
