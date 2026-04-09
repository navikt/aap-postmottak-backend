package no.nav.aap

import no.nav.aap.postmottak.gateway.EnhetsregisterOrganisasjonResponse
import no.nav.aap.postmottak.gateway.EnhetsregisteretGateway
import no.nav.aap.postmottak.gateway.Organisasjonsnummer

object FakeEnhetsregisteretKlient : EnhetsregisteretGateway {
    override fun hentOrganisasjon(organisasjonsnummer: Organisasjonsnummer): EnhetsregisterOrganisasjonResponse? = null
}
