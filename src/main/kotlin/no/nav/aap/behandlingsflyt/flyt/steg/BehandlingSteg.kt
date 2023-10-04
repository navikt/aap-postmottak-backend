package no.nav.aap.behandlingsflyt.flyt.steg

interface BehandlingSteg {

    fun utfør(input: StegInput): StegResultat

    fun type(): StegType

    fun vedTilbakeføring(input: StegInput) {

    }
}
