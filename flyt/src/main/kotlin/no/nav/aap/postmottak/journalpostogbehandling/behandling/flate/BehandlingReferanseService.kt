package no.nav.aap.postmottak.journalpostogbehandling.behandling.flate

import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(BehandlingReferanseService::class.java)

class BehandlingReferanseService(private val behandlingRepository: BehandlingRepository) {
    fun behandling(referanse: Behandlingsreferanse): Behandling {
        try {
            return behandlingRepository.hent(referanse)
        } catch (_: NoSuchElementException) {
            logger.info("Fant ikke behandling med ref $referanse.")
            throw VerdiIkkeFunnetException("Fant ikke behandling med ref $referanse.")
        }
    }
}
