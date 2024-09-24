package no.nav.aap.postmottak.kontrakt.steg

enum class StegGruppe(val skalVises: Boolean, val obligatoriskVisning: Boolean) {
    KATEGORISER(true, true),
    DIGITALISER(true, true),
    AVKLAR_TEMA(true, true),
    START_BEHANDLING(false, false),
    UDEFINERT(false, true),
    SETT_FAGSAK(false, false),
    ENDELIG_JOURNALFØRING(false, false),
    OVERLEVER_TIL_FAGSYSTEM(false, false),
    FINN_SAK(false, false),
}
