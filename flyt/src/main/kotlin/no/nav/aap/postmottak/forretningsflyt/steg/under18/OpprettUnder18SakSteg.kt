package no.nav.aap.postmottak.forretningsflyt.steg.under18

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.steg.StegType

class OpprettUnder18SakSteg : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg = OpprettUnder18SakSteg()

        override fun type(): StegType = StegType.OPPRETT_UNDER18_SAK
    }

    override fun utfør(kontekst: FlytKontekst): StegResultat = error(
        "OpprettUnder18SakSteg skal opprette Kelvin-sak, knytte journalposter med søknader " +
            "og registrerte samtykkejournalposter til saken, " +
            "og opprette eller gjenbruke dokumentbehandlinger med prosesseringsjobber sortert etter mottakstid"
    )
}
