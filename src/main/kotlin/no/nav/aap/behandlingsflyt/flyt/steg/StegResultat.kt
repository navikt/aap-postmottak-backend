package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.StegType
import no.nav.aap.behandlingsflyt.flyt.kontroll.Fortsett
import no.nav.aap.behandlingsflyt.flyt.kontroll.FunnetAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.kontroll.Tilbakeført
import no.nav.aap.behandlingsflyt.flyt.kontroll.TilbakeførtTilAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.kontroll.Transisjon

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
