package no.nav.aap.postmottak.fordeler.arena.jobber

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.Ident


data class ArenaVideresenderKontekst(
    val journalpostId: JournalpostId,
    val ident: Ident,
    val hoveddokumenttittel: String,
    val vedleggstittler: List<String>,
    val navEnhet: String,
)

private val objectMapper = ObjectMapper()

fun JobbInput.getArenaVideresenderKontekst() = objectMapper.readValue(this.payload(), ArenaVideresenderKontekst::class.java)
fun JobbInput.medArenaVideresenderKontekst(arenaVideresender: ArenaVideresenderKontekst): JobbInput {
    objectMapper.writeValueAsString(arenaVideresender)
    return this
}