package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat

class GeneriskPlaceholderSteg(private val stegType: StegType) : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        return StegResultat() // DO NOTHING
    }

    override fun type(): StegType {
        return stegType
    }
}
