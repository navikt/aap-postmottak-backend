package no.nav.aap.postmottak.gateway

import no.nav.aap.komponenter.gateway.Gateway
import java.time.LocalDateTime

interface DoksikkerhetsnettGateway : Gateway {
    fun finnMottatteJournalposterEldreEnn(antallDagerGamle: Int): List<JournalpostFraDoksikkerhetsnett>
}

data class JournalpostFraDoksikkerhetsnett(
    val journalpostId: Long,
    val journalstatus: String,
    val mottakskanal: String?,
    val tema: String,
    val behandlingstema: String?,
    val journalforendeEnhet: String,
    val datoOpprettet: LocalDateTime,
)