package no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.kontrakt.steg.StegType

class AvsluttBehandlingSteg private constructor() : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : BehandlingSteg {
            return AvsluttBehandlingSteg()
        }

        override fun type(): StegType {
            return StegType.IVERKSETTES
        }
    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        // Obligatorisk startsteg for alle flyter
        return Fullført
    }
    
}
