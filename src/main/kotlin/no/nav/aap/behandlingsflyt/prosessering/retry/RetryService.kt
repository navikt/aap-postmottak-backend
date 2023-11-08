package no.nav.aap.behandlingsflyt.prosessering.retry

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.prosessering.OppgaveStatus
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class RetryService(connection: DBConnection) {
    private val log = LoggerFactory.getLogger(RetryService::class.java)
    private val repository = RetryFeiledeOppgaverRepository(connection)

    fun enable() {
        val planlagteFeilhåndteringOppgaver = repository.planlagteFeilhåndteringOppgaver()
        if (planlagteFeilhåndteringOppgaver.isEmpty()) {
            repository.planleggNyKjøring(LocalDateTime.now())
        } else if (!harPlanlagtKjøring(planlagteFeilhåndteringOppgaver)) {
            planlagteFeilhåndteringOppgaver.filter { it.status == OppgaveStatus.FEILET }
                .forEach { repository.markerSomKlar(it) }
        }
        log.info("Planlagt kjøring av feilhåndteringsOppgave")
    }

    private fun harPlanlagtKjøring(planlagteFeilhåndteringOppgaver: List<RetryFeiledeOppgaverRepository.FeilhåndteringOppgaveStatus>): Boolean {
        return planlagteFeilhåndteringOppgaver.any { it.status == OppgaveStatus.KLAR }
    }
}