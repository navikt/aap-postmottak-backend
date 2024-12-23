package no.nav.aap.postmottak.flyt.flate.visning

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe

class DigitaliserVisningUtleder(connection: DBConnection): StegGruppeVisningUtleder {
    private val repositoryProvider = RepositoryProvider(connection)
    private val journalpostRepository = repositoryProvider.provide(JournalpostRepository::class)
    
    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val journalpost = journalpostRepository.hentHvisEksisterer(behandlingId)
        return journalpost?.erDigitalSøknad() == false
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.DIGITALISER
    }
}