package no.nav.aap.postmottak.fordeler.arena.jobber

import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.Ident


data class AutomatiskJournalføringKontekst(
    val journalpostId: JournalpostId,
    val ident: Ident,
    val saksnummer: String,
)


fun JobbInput.getAutomatiskJournalføringKontekst() = DefaultJsonMapper.fromJson(this.payload(), AutomatiskJournalføringKontekst::class.java)
fun JobbInput.medAutomatiskJournalføringKontekst(arenaVideresender: AutomatiskJournalføringKontekst): JobbInput {
    this.medPayload(DefaultJsonMapper.toJson(arenaVideresender))
    return this
}