package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

interface AvklaringsbehoveneDecorator {

    fun alle(): List<Avklaringsbehov>

    fun erSattPåVent(): Boolean
    fun hentBehovForDefinisjon(definisjon: Definisjon): Avklaringsbehov?
    fun skalTilbakeføresEtterKvalitetssikring(): Boolean
}
