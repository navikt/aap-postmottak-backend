package no.nav.aap.postmottak.kontrakt.steg

public enum class StegGruppe(public val skalVises: Boolean, public val obligatoriskVisning: Boolean) {
    KATEGORISER(true, true),
    DIGITALISER(true, false),
    AVKLAR_TEMA(true, true),
    START_BEHANDLING(false, false),
    UDEFINERT(false, true),
    SETT_FAGSAK(false, false),
    ENDELIG_JOURNALFØRING(false, false),
    OVERLEVER_TIL_FAGSYSTEM(true, true),
    AVKLAR_SAK(true, true),
    VIDERESEND(false, false),
    IVERKSETTES(false, false)
}
