package no.nav.aap.postmottak.flyt.steg.internal

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.StegKonstruktør

class StegKonstruktørImpl(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider
) : StegKonstruktør {
    override fun konstruer(steg: FlytSteg): BehandlingSteg {
        return steg.konstruer(repositoryProvider, gatewayProvider)
    }
}