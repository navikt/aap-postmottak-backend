package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class FritakMeldepliktSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        return StegResultat()
    }

    override fun type(): StegType {
        return StegType.FRITAK_MELDEPLIKT
    }
}
