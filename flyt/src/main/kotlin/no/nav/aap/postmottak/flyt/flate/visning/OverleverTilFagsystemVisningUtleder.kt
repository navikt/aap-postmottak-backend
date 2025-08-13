package no.nav.aap.postmottak.flyt.flate.visning

import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe

@Suppress("unused") // Reflection
class OverleverTilFagsystemVisningUtleder(private val journalpostRepository: JournalpostRepository) :
    StegGruppeVisningUtleder {

    constructor(repositoryProvider: RepositoryProvider) : this(
        repositoryProvider.provide()
    )

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        // TODO: Oppdater med øvrige dokumenter som skal sendes til behandlingsflyt 
        return journalpostRepository.hentHvisEksisterer(behandlingId)?.erSøknad() == true
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.OVERLEVER_TIL_FAGSYSTEM
    }
}