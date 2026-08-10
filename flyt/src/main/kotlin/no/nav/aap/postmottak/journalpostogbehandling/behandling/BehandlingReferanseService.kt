package no.nav.aap.postmottak.journalpostogbehandling.behandling

import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import org.slf4j.LoggerFactory

class BehandlingReferanseService(private val behandlingRepository: BehandlingRepository) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun behandling(referanse: Behandlingsreferanse): Behandling {
        try {
            return behandlingRepository.hent(referanse)
        } catch (_: NoSuchElementException) {
            logger.info("Fant ikke behandling med ref $referanse.")
            throw VerdiIkkeFunnetException("Fant ikke behandling med ref $referanse.")
        }
    }
}
