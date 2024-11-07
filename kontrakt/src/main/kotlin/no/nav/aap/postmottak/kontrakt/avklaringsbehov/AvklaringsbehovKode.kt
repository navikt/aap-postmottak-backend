package no.nav.aap.postmottak.kontrakt.avklaringsbehov

/**
 * Disse verdiene må igjen gjenspeile enumene under
 */
const val MANUELT_SATT_PÅ_VENT_KODE = "9001"
const val KATEGORISER_DOKUMENT_KODE = "1337"
const val DIGITALISER_DOKUMENT_KODE = "1338"
const val AVKLAR_TEMA_KODE = "1339"
const val AVKLAR_SAKSNUMMER_KODE = "1340"
const val ENDRE_TEMA_KODE = "1341"

enum class AvklaringsbehovKode {
    `9001`,
    `1337`,
    `1338`,
    `1339`,
    `1340`,
    `1341`
}