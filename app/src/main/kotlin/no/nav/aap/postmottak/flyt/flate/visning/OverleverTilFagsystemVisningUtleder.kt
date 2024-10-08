package no.nav.aap.postmottak.flyt.flate.visning

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.sakogbehandling.behandling.JournalpostRepositoryImpl
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class OverleverTilFagsystemVisningUtleder(connection: DBConnection): StegGruppeVisningUtleder {

    private val journalpostRepository = JournalpostRepositoryImpl(connection)
    
    override fun skalVises(behandlingId: BehandlingId): Boolean {
        // TODO: Oppdater med øvrige dokumenter som skal sendes til behandlingsflyt 
        return journalpostRepository.hentHvisEksisterer(behandlingId)?.erSøknad() == true
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.OVERLEVER_TIL_FAGSYSTEM
    }
}