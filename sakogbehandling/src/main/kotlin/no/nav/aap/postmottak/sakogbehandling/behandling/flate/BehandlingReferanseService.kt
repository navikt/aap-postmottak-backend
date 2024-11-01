package no.nav.aap.postmottak.sakogbehandling.behandling.flate

import no.nav.aap.postmottak.sakogbehandling.behandling.Behandling
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(BehandlingReferanseService::class.java)

class BehandlingReferanseService(private val behandlingRepositoryImpl: BehandlingRepository) {
    fun behandling(referanse: Behandlingsreferanse): Behandling {
        try {
            return behandlingRepositoryImpl.hent(referanse)
        } catch (e: NoSuchElementException) {
            logger.info("Fant ikke behandling med ref $referanse. Stacktrace: ${e.stackTraceToString()}")
            throw ElementNotFoundException()
        }
    }

    fun finnJournalpostId(behandlingsreferanse: BehandlingsreferansePathParam) = behandlingRepositoryImpl
        .hent(behandlingsreferanse).journalpostId
}