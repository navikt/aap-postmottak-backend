package no.nav.aap.behandlingsflyt.avklaringsbehov.flate

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.Endring

class DefinisjonEndring(val definisjon: Definisjon, val endring: Endring) : Comparable<DefinisjonEndring> {
    override fun compareTo(other: DefinisjonEndring): Int {
        return endring.compareTo(other.endring)
    }
}