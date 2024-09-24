package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

interface AvklaringsbehoveneDecorator {

    fun alle(): List<Avklaringsbehov>

    fun hentBehovForDefinisjon(definisjon: Definisjon): Avklaringsbehov?
    fun skalTilbakeføresEtterKvalitetssikring(): Boolean
}
