package no.nav.aap.postmottak.fordeler.arena.jobber

import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.Ident


data class ArenaVideresenderKontekst(
    val journalpostId: JournalpostId,
    val ident: Ident,
    val hoveddokumenttittel: String,
    val vedleggstitler: List<String>,
    val navEnhet: String
)

fun JobbInput.getArenaVideresenderKontekst() = DefaultJsonMapper.fromJson(this.payload(), ArenaVideresenderKontekst::class.java)
fun JobbInput.medArenaVideresenderKontekst(arenaVideresender: ArenaVideresenderKontekst): JobbInput {
    return this.medPayload(DefaultJsonMapper.toJson(arenaVideresender))
}