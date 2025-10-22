package no.nav.aap.postmottak.avklaringsbehov

import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

interface AvklaringsbehoveneDecorator {
    fun alle(): List<Avklaringsbehov>
    fun alleEkskludertVentebehov(): List<Avklaringsbehov>

    fun erSattPåVent(): Boolean
    fun hentBehovForDefinisjon(definisjon: Definisjon): Avklaringsbehov?
}
