package no.nav.aap.verdityper.flyt

import no.nav.aap.verdityper.sakogbehandling.Status

enum class StegType(val gruppe: StegGruppe, val status: Status, val tekniskSteg: Boolean = false) {
    KATEGORISER_DOKUMENT(
        gruppe = StegGruppe.KATEGORISER,
        status = Status.UTREDES
    ),
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
    ENDERLIG_JOURNALFØRING(
        gruppe = StegGruppe.ENDERLIG_JOURNALFØRING,
        status = Status.UTREDES,
    ),
    OVERLEVER_TIL_FAGSYSTEM(
        gruppe = StegGruppe.OVERLEVER_TIL_FAGSYSTEM,
        status = Status.AVSLUTTET,
    ),
    UDEFINERT(
        gruppe = StegGruppe.UDEFINERT, status = Status.UTREDES
    ), // Forbeholdt deklarasjon for avklaringsbehov som
    FINN_SAK(
        gruppe = StegGruppe.FINN_SAK,
        status = Status.UTREDES,
    ),
    GROVKATEGORTISER_DOKUMENT(
        gruppe = StegGruppe.GROVKATEGORISERING,
        status = Status.UTREDES,
    ),


}
