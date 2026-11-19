package no.nav.aap.postmottak.forretningsflyt.steg.under18

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.steg.StegType

class AvklarUnder18Steg : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg = AvklarUnder18Steg()

        override fun type(): StegType = StegType.AVKLAR_UNDER18
    }

    override fun utfør(kontekst: FlytKontekst): StegResultat = error(
        "AvklarUnder18Steg skal journalføre på generell sak og avklare henleggelse eller videre søknad"
    )
}
