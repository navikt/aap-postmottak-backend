package no.nav.aap.postmottak.sakogbehandling.behandling.flate

import no.nav.aap.postmottak.sakogbehandling.behandling.Behandling
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(BehandlingReferanseService::class.java)

class BehandlingReferanseService(private val behandlingRepositoryImpl: BehandlingRepository) {
    fun behandling(journalpostId: JournalpostId): Behandling {
        try {
            return behandlingRepositoryImpl.hent(journalpostId)
        } catch (e: NoSuchElementException) {
            logger.info("Fant ikke behandling med ref $journalpostId. Stacktrace: ${e.stackTraceToString()}")
            throw ElementNotFoundException()
        }
    }
}