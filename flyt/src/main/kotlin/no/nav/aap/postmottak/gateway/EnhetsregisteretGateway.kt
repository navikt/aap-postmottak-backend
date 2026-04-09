package no.nav.aap.postmottak.gateway

import no.nav.aap.komponenter.gateway.Gateway

@JvmInline
value class Organisasjonsnummer(val value: String)

interface EnhetsregisteretGateway : Gateway {
    fun hentOrganisasjon(organisasjonsnummer: Organisasjonsnummer): EnhetsregisterOrganisasjonResponse?
}

data class EnhetsregisterOrganisasjonResponse(
    val organisasjonsnummer: String,
    val navn: EnhetsregisterOrganisasjonsNavn? = null,
    val fantIkke: Boolean = false
)

data class EnhetsregisterOrganisasjonsNavn(
    val sammensattnavn: String
)
