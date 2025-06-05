package no.nav.aap.fordeler.arena.jobber

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

data class AutomatiskJournalføringKontekst(
    override val journalpostId: JournalpostId,
    override val innkommendeJournalpostId: Long,
    val ident: Ident,
    val saksnummer: String,
) : ArenaBaseKontekst(journalpostId, innkommendeJournalpostId) {}


fun JobbInput.getAutomatiskJournalføringKontekst() = DefaultJsonMapper.fromJson(this.payload(), AutomatiskJournalføringKontekst::class.java)
fun JobbInput.medAutomatiskJournalføringKontekst(arenaVideresender: AutomatiskJournalføringKontekst): JobbInput {
    this.forSak(arenaVideresender.journalpostId.referanse)
    this.medPayload(DefaultJsonMapper.toJson(arenaVideresender))
    return this
}