package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon

class StegResultat(val avklaringsbehov: List<Definisjon> = listOf(),
                   val tilbakeførtTilSteg: StegType = StegType.UDEFINERT) {

    fun transisjon(): Transisjon {
        if (avklaringsbehov.isNotEmpty() && tilbakeførtTilSteg != StegType.UDEFINERT) {
            return TilbakeførtTilAvklaringsbehov(avklaringsbehov, tilbakeførtTilSteg)
        }
        if (avklaringsbehov.isNotEmpty()) {
            return FunnetAvklaringsbehov(avklaringsbehov)
        }
        if (tilbakeførtTilSteg != StegType.UDEFINERT) {
            return Tilbakeført(tilbakeførtTilSteg)
        }
        return Fortsett
    }
}
