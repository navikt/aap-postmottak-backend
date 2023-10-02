package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.flyt.StegType

interface BehandlingSteg {

    fun utfør(input: StegInput): StegResultat

    fun type(): StegType

    fun vedTilbakeføring(input: StegInput) {

    }
}
