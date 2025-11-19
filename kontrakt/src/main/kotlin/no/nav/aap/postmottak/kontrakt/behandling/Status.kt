package no.nav.aap.postmottak.kontrakt.behandling

public enum class Status {
    OPPRETTET,
    UTREDES,
    IVERKSETTES,
    AVSLUTTET;

    public fun erAvsluttet(): Boolean {
        return AVSLUTTET == this || IVERKSETTES == this
    }

    public fun erÅpen(): Boolean = !erAvsluttet()
}
