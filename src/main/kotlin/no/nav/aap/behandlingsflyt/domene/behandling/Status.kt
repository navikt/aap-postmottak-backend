package no.nav.aap.behandlingsflyt.domene.behandling

enum class Status {
    OPPRETTET,
    UTREDES,
    AVSLUTTET,
    PÅ_VENT;

    fun erAvsluttet(): Boolean {
        return AVSLUTTET == this
    }
}
