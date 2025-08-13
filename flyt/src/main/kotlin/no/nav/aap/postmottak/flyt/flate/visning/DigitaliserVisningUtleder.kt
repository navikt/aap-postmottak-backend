package no.nav.aap.postmottak.flyt.flate.visning

import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe

class DigitaliserVisningUtleder(repositoryProvider: RepositoryProvider): StegGruppeVisningUtleder {
    private val journalpostRepository = repositoryProvider.provide(JournalpostRepository::class)
    
    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val journalpost = journalpostRepository.hentHvisEksisterer(behandlingId) ?: throw IllegalStateException("Journalpost mangler")
        return !(journalpost.erDigitalSøknad() || journalpost.erDigitalLegeerklæring() || journalpost.erDigitaltMeldekort())
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.DIGITALISER
    }
}