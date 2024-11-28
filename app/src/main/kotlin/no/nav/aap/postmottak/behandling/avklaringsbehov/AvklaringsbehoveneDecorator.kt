package no.nav.aap.postmottak.behandling.avklaringsbehov

import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

interface AvklaringsbehoveneDecorator {

    fun alle(): List<Avklaringsbehov>

    fun erSattPåVent(): Boolean
    fun hentBehovForDefinisjon(definisjon: Definisjon): Avklaringsbehov?
}
