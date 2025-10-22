package no.nav.aap.postmottak.kontrakt.behandling

enum class Status {
    OPPRETTET,
    UTREDES,
    IVERKSETTES,
    AVSLUTTET;

    fun erAvsluttet(): Boolean {
        return AVSLUTTET == this || IVERKSETTES == this
    }

    fun erÅpen(): Boolean = !erAvsluttet()
}
