package no.nav.aap.verdityper

enum class Brevkoder(val kode: String) {
    SØKNAD("NAV 11-13.05"),
    STANDARD_ETTERSENDING("NAVe 11-13.05"),
    LEGEERKLÆRING("NAV 08-07.08"),
    SØKNAD_OM_REISESTØNAD("NAV 11-12.05"),
    SØKNAD_OM_REISESTØNAD_ETTERSENDELSE("NAVe 11-12.05"),
    ANNEN("ANNEN")
}