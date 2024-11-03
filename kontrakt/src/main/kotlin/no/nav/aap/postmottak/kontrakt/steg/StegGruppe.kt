package no.nav.aap.postmottak.kontrakt.steg

enum class StegGruppe(val skalVises: Boolean, val obligatoriskVisning: Boolean) {
    ROUTING(false, false),
    KATEGORISER(true, true),
    DIGITALISER(true, false),
    AVKLAR_TEMA(true, true),
    START_BEHANDLING(false, false),
    UDEFINERT(false, true),
    SETT_FAGSAK(false, false),
    ENDELIG_JOURNALFØRING(false, false),
    OVERLEVER_TIL_FAGSYSTEM(true, false),
    AVKLAR_SAK(true, true),
    ENDRE_TEMA(true, false),
    VIDERESEND(false, false)
}
