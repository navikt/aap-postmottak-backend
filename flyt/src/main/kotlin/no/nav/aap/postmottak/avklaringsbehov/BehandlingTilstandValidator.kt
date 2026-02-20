package no.nav.aap.postmottak.avklaringsbehov

import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingReferanseService
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører

class BehandlingTilstandValidator(
    private val behandlingReferanseService: BehandlingReferanseService,
    private val flytJobbRepository: FlytJobbRepository
) {
    fun validerTilstand(referanse: Behandlingsreferanse, behandlingVersjon: Long) {
        val behandling = behandlingReferanseService.behandling(referanse)
        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, behandlingVersjon)

        val jobberForBehandling = flytJobbRepository.hentJobberForBehandling(behandling.id.toLong())
            .filter { it.type() == ProsesserBehandlingJobbUtfører.type }

        if (jobberForBehandling.isNotEmpty()) {
            throw BehandlingUnderProsesseringException()
        }
    }
}