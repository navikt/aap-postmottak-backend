package no.nav.aap.postmottak.journalpostogbehandling.behandling.flate

import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(BehandlingReferanseService::class.java)

class BehandlingReferanseService(private val behandlingRepository: BehandlingRepository) {
    fun behandling(referanse: Behandlingsreferanse): Behandling {
        try {
            return behandlingRepository.hent(referanse)
        } catch (e: NoSuchElementException) {
            logger.info("Fant ikke behandling med ref $referanse. Stacktrace: ${e.stackTraceToString()}")
            throw ElementNotFoundException()
        }
    }

    fun finnJournalpostId(behandlingsreferanse: BehandlingsreferansePathParam) = behandlingRepository
        .hent(behandlingsreferanse).journalpostId
}