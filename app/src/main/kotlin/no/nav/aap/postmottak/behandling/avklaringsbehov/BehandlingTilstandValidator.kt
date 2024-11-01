package no.nav.aap.postmottak.behandling.avklaringsbehov

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandlingsreferanse

class BehandlingTilstandValidator(connection: DBConnection) {
    private val flytJobbRepository = FlytJobbRepository(connection)
    private val behandlingReferanseService = BehandlingReferanseService(BehandlingRepositoryImpl(connection))

    fun validerTilstand(referanse: Behandlingsreferanse, behandlingVersjon: Long) {
        val behandling = behandlingReferanseService.behandling(referanse)
        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, behandlingVersjon)

        val jobberForBehandling = flytJobbRepository.hentJobberForBehandling(behandling.id.toLong())
        if (jobberForBehandling.isNotEmpty()) {
            throw BehandlingUnderProsesseringException()
        }
    }
}