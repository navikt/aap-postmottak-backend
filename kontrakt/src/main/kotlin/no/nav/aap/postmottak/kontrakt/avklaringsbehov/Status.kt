package no.nav.aap.postmottak.kontrakt.avklaringsbehov

enum class Status {
    OPPRETTET,
    AVSLUTTET,
    SENDT_TILBAKE_FRA_BESLUTTER,
    AVBRUTT;

    fun erÅpent(): Boolean {
        return this in setOf(
            OPPRETTET,
            SENDT_TILBAKE_FRA_BESLUTTER
        )
    }

    fun erAvsluttet(): Boolean {
        return this in setOf(
            AVSLUTTET,
            AVBRUTT
        )
    }
}
