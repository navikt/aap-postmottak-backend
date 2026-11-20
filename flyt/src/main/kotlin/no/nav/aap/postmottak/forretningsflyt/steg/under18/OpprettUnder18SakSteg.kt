package no.nav.aap.postmottak.forretningsflyt.steg.under18

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.under18.Under18Aldersgruppe
import no.nav.aap.postmottak.gateway.Klokke
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.hentFødselsdatoFor

class OpprettUnder18SakSteg(
    private val under18Repository: Under18Repository,
    private val klokke: Klokke,
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg = OpprettUnder18SakSteg(
            repositoryProvider.provide(),
            gatewayProvider.provide(),
        )

        override fun type(): StegType = StegType.OPPRETT_UNDER18_SAK
    }

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val aldersgruppe = Under18Aldersgruppe.vurder(
            under18Repository.hentFødselsdatoFor(kontekst.behandlingId),
            klokke.dagensDato()
        )
        val avklaring = under18Repository.hentSøknadsavklaring(kontekst.behandlingId)
        if (avklaring?.erHenlagt(aldersgruppe) == true) {
            return Fullført
        }

        error(
            "OpprettUnder18SakSteg skal opprette Kelvin-sak, knytte journalpostene til saken " +
                "og opprette eller gjenbruke dokumentbehandlinger med prosesseringsjobber sortert etter mottakstid"
        )
    }
}
