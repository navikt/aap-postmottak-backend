package no.nav.aap.domene.behandling

enum class Status {
    OPPRETTET,
    UTREDES,
    AVSLUTTET,
    PÅ_VENT;

    fun erAvsluttet(): Boolean {
        return AVSLUTTET == this
    }
}
