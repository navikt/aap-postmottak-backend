package no.nav.aap.behandlingsflyt.avklaringsbehov.flate

import java.time.LocalDateTime

class Historikk(val aksjon: Aksjon, val tidspunkt: LocalDateTime, val avIdent: String) : Comparable<Historikk> {
    override fun compareTo(other: Historikk): Int {
        return tidspunkt.compareTo(other.tidspunkt)
    }

}
