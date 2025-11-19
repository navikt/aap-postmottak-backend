package no.nav.aap.postmottak.kontrakt.avklaringsbehov

/**
 * Disse verdiene må igjen gjenspeile enumene under.
 */
public const val MANUELT_SATT_PÅ_VENT_KODE: String = "9001"

@Deprecated("Bruk heller `DIGITALISER_DOKUMENT_KODE`.")
public const val KATEGORISER_DOKUMENT_KODE: String = "1337"
public const val DIGITALISER_DOKUMENT_KODE: String = "1338"
public const val AVKLAR_TEMA_KODE: String = "1339"
public const val AVKLAR_SAKSNUMMER_KODE: String = "1340"
public const val AVKLAR_OVERLEVERING_KODE: String = "1341"

public enum class AvklaringsbehovKode {
    `9001`,
    `1337`,
    `1338`,
    `1339`,
    `1340`,
    `1341`,
    `1342`
}
