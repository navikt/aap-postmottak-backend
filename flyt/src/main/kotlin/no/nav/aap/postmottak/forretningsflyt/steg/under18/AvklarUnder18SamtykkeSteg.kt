package no.nav.aap.postmottak.forretningsflyt.steg.under18

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.steg.StegType

class AvklarUnder18SamtykkeSteg : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg = AvklarUnder18SamtykkeSteg()

        override fun type(): StegType = StegType.AVKLAR_UNDER18_SAMTYKKE
    }

    override fun utfør(kontekst: FlytKontekst): StegResultat = error(
        "AvklarUnder18SamtykkeSteg skal avklare samtykke og håndtere samtykkevent"
    )
}
