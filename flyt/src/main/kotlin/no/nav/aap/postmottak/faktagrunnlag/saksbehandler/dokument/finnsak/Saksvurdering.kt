package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak

data class Saksvurdering(
    val saksnummer: String? = null,
    val opprettNySak: Boolean = false,
    val generellSak: Boolean = false,
) {
    init {
        require(saksnummer != null || opprettNySak || generellSak) { "Sak må oppgis"}
    }
}