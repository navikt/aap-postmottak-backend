package no.nav.aap.postmottak.flyt.steg

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.kontrakt.steg.StegType

interface FlytSteg {
    fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg

    fun type(): StegType
}
