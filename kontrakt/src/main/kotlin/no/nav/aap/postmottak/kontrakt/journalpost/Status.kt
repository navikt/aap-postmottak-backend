package no.nav.aap.postmottak.kontrakt.journalpost

enum class Status {
    OPPRETTET,
    UTREDES,
    AVSLUTTET;

    fun erAvsluttet(): Boolean {
        return AVSLUTTET == this
    }
}
