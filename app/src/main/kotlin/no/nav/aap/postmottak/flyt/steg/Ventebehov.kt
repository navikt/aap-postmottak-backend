package no.nav.aap.postmottak.flyt.steg

import no.nav.aap.postmottak.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import java.time.LocalDate

data class Ventebehov(val definisjon: Definisjon, val grunn: ÅrsakTilSettPåVent, val frist: LocalDate? = null) {
    init {
        if (frist != null) {
            require(frist.isAfter(LocalDate.now()))
        }
    }
}