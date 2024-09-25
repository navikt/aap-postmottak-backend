package no.nav.aap.postmottak.kontrakt.journalpost

enum class Status {
    OPPRETTET,
    UTREDES,
    IVERKSETTES,
    AVSLUTTET;

    fun erAvsluttet(): Boolean {
        return AVSLUTTET == this || IVERKSETTES == this
    }
}
