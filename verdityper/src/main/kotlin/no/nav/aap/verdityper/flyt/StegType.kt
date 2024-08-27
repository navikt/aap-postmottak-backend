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
        status = Status.OPPRETTET
    ),
    UDEFINERT(
        gruppe = StegGruppe.UDEFINERT, status = Status.UTREDES, tekniskSteg = true
    ), // Forbeholdt deklarasjon for avklaringsbehov som


}
