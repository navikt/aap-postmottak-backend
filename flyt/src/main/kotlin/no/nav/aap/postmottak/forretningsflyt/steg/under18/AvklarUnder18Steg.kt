package no.nav.aap.postmottak.forretningsflyt.steg.under18

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.under18.Under18Aldersgruppe
import no.nav.aap.postmottak.gateway.Klokke
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.hentFødselsdatoFor

class AvklarUnder18Steg(
    private val under18Repository: Under18Repository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val klokke: Klokke,
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg = AvklarUnder18Steg(
            repositoryProvider.provide(),
            AvklaringsbehovService(repositoryProvider),
            gatewayProvider.provide(),
        )

        override fun type(): StegType = StegType.AVKLAR_UNDER18
    }

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val fødselsdato = under18Repository.hentFødselsdatoFor(kontekst.behandlingId)
        val påkrevdSituasjon = Under18Aldersgruppe.vurder(fødselsdato, klokke.dagensDato()).påkrevdSøknadssituasjon
        val avklaring = under18Repository.hentSøknadsavklaring(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.AVKLAR_UNDER18_SØKNAD,
            vedtakBehøverVurdering = { påkrevdSituasjon != null },
            erTilstrekkeligVurdert = { avklaring?.situasjon == påkrevdSituasjon },
            kontekst = kontekst
        )

        return Fullført
    }
}
