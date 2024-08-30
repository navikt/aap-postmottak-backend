package no.nav.aap.verdityper.flyt

enum class StegGruppe(val skalVises: Boolean, val obligatoriskVisning: Boolean) {
    KATEGORISER(true, true),
    DIGITALISER(true, true),
    START_BEHANDLING(false, false),
    UDEFINERT(false, true),
    AVKLAR_TEMA(true, true),
    ENDERLIG_JOURNALFØRING(false, false),
    OVERLEVER_TIL_FAGSYSTEM(false, false),
    FINN_SAK(false, false),
    GROVKATEGORISERING(true, true),
}
