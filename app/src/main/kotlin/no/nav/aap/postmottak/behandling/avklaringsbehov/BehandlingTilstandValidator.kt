package no.nav.aap.postmottak.behandling.avklaringsbehov

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.motor.FlytJobbRepository

class BehandlingTilstandValidator(connection: DBConnection) {
    private val flytJobbRepository = FlytJobbRepository(connection)
    private val behandlingReferanseService = BehandlingReferanseService(BehandlingRepositoryImpl(connection))

    fun validerTilstand(journalpostId: JournalpostId, behandlingVersjon: Long) {
        val behandling = behandlingReferanseService.behandling(journalpostId)
        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, behandlingVersjon)

        val jobberForBehandling = flytJobbRepository.hentJobberForBehandling(behandling.id.toLong())
        if (jobberForBehandling.isNotEmpty()) {
            throw BehandlingUnderProsesseringException()
        }
    }
}