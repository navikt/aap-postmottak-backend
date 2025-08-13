package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst

class InformasjonskravGrunnlagImpl(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider
) : InformasjonskravGrunnlag {

    override fun oppdaterFaktagrunnlagForKravliste(
        kravliste: List<Informasjonskravkonstruktør>,
        kontekst: FlytKontekst
    ): List<Informasjonskravkonstruktør> {
        // Hva gir dette leddet?
        return kravliste.filterNot { kravtype ->
            kravtype.konstruer(repositoryProvider, gatewayProvider).oppdater(kontekst) == Informasjonskrav.Endret.ENDRET
        }
    }
}
