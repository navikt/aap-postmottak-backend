package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak

data class Saksvurdering(
    val saksnummer: String? = null,
    val generellSak: Boolean = false,
) {
    init {
        require(saksnummer != null || generellSak) { "Sak må oppgis"}
    }
}