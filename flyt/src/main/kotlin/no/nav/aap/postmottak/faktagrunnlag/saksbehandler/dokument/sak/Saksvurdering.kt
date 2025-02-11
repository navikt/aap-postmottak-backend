package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak

data class Saksvurdering(
    val saksnummer: String? = null,
    val generellSak: Boolean = false,
) {
    init {
        require(!saksnummer.isNullOrEmpty() || generellSak) { "Sak må oppgis"}
    }
}