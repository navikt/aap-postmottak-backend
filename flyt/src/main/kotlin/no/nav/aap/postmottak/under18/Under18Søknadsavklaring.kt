package no.nav.aap.postmottak.under18

import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException

enum class Under18Søknadssituasjon {
    SØKER_FOR_UNG,
    SØKER_FYLT_18,
}

/**
 * Svaret saksbehandler registrerer i AVKLAR_UNDER18_SØKNAD. Situasjonen skiller en bekreftet henleggelse
 * for en for ung søker fra svaret på om en søker som har fylt 18 år, vil fortsette.
 */
data class Under18Søknadsavklaring(
    val situasjon: Under18Søknadssituasjon,
    val søkerVilFortsette: Boolean,
) {
    init {
        if (situasjon == Under18Søknadssituasjon.SØKER_FOR_UNG && søkerVilFortsette) {
            throw UgyldigForespørselException("En søker som er for ung, kan ikke fortsette søknaden")
        }
    }

    fun erHenlagt(aldersgruppe: Under18Aldersgruppe): Boolean =
        situasjon == aldersgruppe.påkrevdSøknadssituasjon && !søkerVilFortsette
}
