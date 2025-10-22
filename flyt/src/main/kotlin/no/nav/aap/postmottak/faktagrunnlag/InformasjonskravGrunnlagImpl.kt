package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.steg.StegType

class InformasjonskravGrunnlagImpl(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider
) : InformasjonskravGrunnlag {

    override fun oppdaterFaktagrunnlagForKravliste(
        kravkonstruktører: List<Pair<StegType, Informasjonskravkonstruktør>>,
        kontekst: FlytKontekst
    ): List<Informasjonskravkonstruktør> {
        // Hva gir dette leddet?
        return kravkonstruktører.filterNot { (_, konstruktør) ->
            konstruktør.konstruer(repositoryProvider, gatewayProvider)
                .oppdater(kontekst) == Informasjonskrav.Endret.ENDRET
        }.map { it.second }
    }
}
