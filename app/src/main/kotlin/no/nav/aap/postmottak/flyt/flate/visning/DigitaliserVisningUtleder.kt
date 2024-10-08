package no.nav.aap.postmottak.flyt.flate.visning

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.sakogbehandling.behandling.JournalpostRepositoryImpl
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class DigitaliserVisningUtleder(connection: DBConnection): StegGruppeVisningUtleder {
    private val journalpostRepository = JournalpostRepositoryImpl(connection)
    
    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val journalpost = journalpostRepository.hentHvisEksisterer(behandlingId)
        return journalpost?.kanBehandlesAutomatisk() == false
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.DIGITALISER
    }
}