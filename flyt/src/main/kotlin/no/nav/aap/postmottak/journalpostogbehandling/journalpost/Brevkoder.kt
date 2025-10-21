package no.nav.aap.postmottak.journalpostogbehandling.journalpost

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType

/**
 * NB: Behandlingstypene er hentet fra felles kodeverk - og må oppdateres dersom kodeverket endres.
 * Behandlingstypene er hentet fra TemaSkjemaRuting.
 * TemaSkjemaGjelder kan også være aktuelt for å hente behandlingstema og behandlingstype
 * TODO: Kan vurdere å integrere mot felleskodeverk
 */
enum class Brevkoder(val kode: String, val behandlingstype: String?) {
    SØKNAD("NAV 11-13.05", null),
    STANDARD_ETTERSENDING("NAVe 11-13.05", null),
    LEGEERKLÆRING("NAV 08-07.08", null),
    SØKNAD_OM_REISESTØNAD("NAV 11-12.05", null),
    SØKNAD_OM_REISESTØNAD_ETTERSENDELSE("NAVe 11-12.05", null),
    ANKE("NAV 90-00.08 A", Behandlingstype.KLAGE.kode),
    KLAGE("NAV 90-00.08 K", Behandlingstype.KLAGE.kode),
    ANKE_ETTERSENDELSE("NAVe 90-00.08 A", null),
    KLAGE_ETTERSENDELSE("NAVe 90-00.08 K", null),
    BREV_UTLAND("UTL", Behandlingstype.UTLAND.kode),
    EGENERKLÆRING_AAP_EØS("NAV 11-03.08", Behandlingstype.EU_EØS_PRAKSISENDRING.kode),
    MELDEKORT("NAV 00-10.02", null),
    MELDEKORT_KORRIGERING("NAV 00-10.03", null),
    ANNEN("ANNEN", null);

    companion object {
        fun fraKode(kode: String) = entries.find { it.kode == kode } ?: ANNEN
    }
}

enum class Behandlingstema(val kode: String) {
    AAP("ab0014");
}

enum class Behandlingstype(val kode: String) {
    KLAGE("ae0058"),
    EU_EØS_PRAKSISENDRING("ae0237"),
    UTLAND("ae0106");
}

object BrevkoderHelper {
    /**
     * Forsøker å mappe brevkode til innsendingstype. Null betyr at
     * man ikke har funnet noen passende innsendingtype.
     */
    fun mapTilInnsendingType(brevkode: String): InnsendingType? {
        val brevkodeTilInnsendingMap = mapOf(
            Brevkoder.SØKNAD to InnsendingType.SØKNAD,
            Brevkoder.LEGEERKLÆRING to InnsendingType.LEGEERKLÆRING,
            Brevkoder.MELDEKORT to InnsendingType.MELDEKORT,
            Brevkoder.MELDEKORT_KORRIGERING to InnsendingType.MELDEKORT,
            Brevkoder.KLAGE to InnsendingType.KLAGE
        )

        return brevkodeTilInnsendingMap[Brevkoder.fraKode(brevkode)]
    }

}