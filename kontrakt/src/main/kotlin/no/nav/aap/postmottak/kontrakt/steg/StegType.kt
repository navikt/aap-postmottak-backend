package no.nav.aap.postmottak.kontrakt.steg

import no.nav.aap.postmottak.kontrakt.behandling.Status

public enum class StegType(
    public val gruppe: StegGruppe,
    public val status: Status,
    public val tekniskSteg: Boolean = false
) {
    DIGITALISER_DOKUMENT(
        gruppe = StegGruppe.DIGITALISER,
        status = Status.UTREDES
    ),
    START_BEHANDLING(
        gruppe = StegGruppe.START_BEHANDLING,
        status = Status.OPPRETTET,
        //tekniskSteg = true
    ),
    AVKLAR_TEMA(
        gruppe = StegGruppe.AVKLAR_TEMA,
        status = Status.UTREDES
    ),
    SETT_FAGSAK(
        gruppe = StegGruppe.SETT_FAGSAK,
        status = Status.UTREDES
    ),
    ENDELIG_JOURNALFØRING(
        gruppe = StegGruppe.ENDELIG_JOURNALFØRING,
        status = Status.UTREDES,
    ),
    OVERLEVER_TIL_FAGSYSTEM(
        gruppe = StegGruppe.OVERLEVER_TIL_FAGSYSTEM,
        status = Status.UTREDES,
    ),
    UDEFINERT(
        gruppe = StegGruppe.UDEFINERT, status = Status.UTREDES
    ), // Forbeholdt deklarasjon for avklaringsbehov som
    AVKLAR_SAK(
        gruppe = StegGruppe.AVKLAR_SAK,
        status = Status.UTREDES,
    ),
    VIDERESEND(
        gruppe = StegGruppe.VIDERESEND,
        status = Status.IVERKSETTES,
    ),
    IVERKSETTES(
        gruppe = StegGruppe.IVERKSETTES,
        status = Status.IVERKSETTES,
    ),
}
