package no.nav.aap.verdityper.flyt

enum class StegGruppe(val skalVises: Boolean, val obligatoriskVisning: Boolean) {
    KATEGORISER(true, true),
    DIGITALISER(true, true),
    START_BEHANDLING(false, true),
    UDEFINERT(false, true),
}
