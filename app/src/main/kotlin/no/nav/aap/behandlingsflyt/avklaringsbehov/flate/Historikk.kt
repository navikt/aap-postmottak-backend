package no.nav.aap.behandlingsflyt.avklaringsbehov.flate

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import java.time.LocalDateTime

class Historikk(val definisjon: Definisjon, val tidspunkt: LocalDateTime, val avIdent: String) : Comparable<Historikk> {
    override fun compareTo(other: Historikk): Int {
        return tidspunkt.compareTo(other.tidspunkt)
    }

}
