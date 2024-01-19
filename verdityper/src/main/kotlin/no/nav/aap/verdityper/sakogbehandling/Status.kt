package no.nav.aap.verdityper.sakogbehandling

enum class Status {
    OPPRETTET,
    UTREDES,
    AVSLUTTET,
    PÅ_VENT;

    fun erAvsluttet(): Boolean {
        return AVSLUTTET == this
    }
}
