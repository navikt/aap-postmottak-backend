package no.nav.aap.postmottak.kontrakt.avklaringsbehov

public enum class Status {
    OPPRETTET,
    AVSLUTTET,
    SENDT_TILBAKE_FRA_BESLUTTER,
    AVBRUTT;

    public fun erÅpent(): Boolean {
        return this in setOf(
            OPPRETTET,
            SENDT_TILBAKE_FRA_BESLUTTER
        )
    }

    public fun erAvsluttet(): Boolean {
        return this in setOf(
            AVSLUTTET,
            AVBRUTT
        )
    }
}
