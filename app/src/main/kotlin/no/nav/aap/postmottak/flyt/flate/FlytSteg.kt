package no.nav.aap.postmottak.flyt.flate

import no.nav.aap.postmottak.kontrakt.steg.StegType


data class FlytSteg(val stegType: StegType, val avklaringsbehov: List<AvklaringsbehovDTO>)