package no.nav.aap.postmottak.fordeler.arena.jobber

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.Ident


data class AutomatiskJournalføringKontekst(
    val journalpostId: JournalpostId,
    val ident: Ident,
    val saksnummer: String,
)

private val objectMapper = ObjectMapper()

fun JobbInput.getAutomatiskJournalføringKontekst() = objectMapper.readValue(this.payload(), AutomatiskJournalføringKontekst::class.java)
fun JobbInput.medAutomatiskJournalføringKontekst(arenaVideresender: AutomatiskJournalføringKontekst): JobbInput {
    objectMapper.writeValueAsString(arenaVideresender)
    return this
}