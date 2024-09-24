package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType

data class DefinisjonDTO(
    val navn: String,
    val type: String,
    val behovType: Definisjon.BehovType,
    val løsesISteg: StegType
)
