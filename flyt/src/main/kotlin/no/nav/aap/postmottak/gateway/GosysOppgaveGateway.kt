package no.nav.aap.postmottak.gateway

import no.nav.aap.lookup.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

interface GosysOppgaveGateway: Gateway {
    fun opprettEndreTemaOppgave(journalpostId: JournalpostId, personident: String)
    fun finnOppgaverForJournalpost(journalpostId: JournalpostId, oppgavetyper: List<Oppgavetype> = listOf(Oppgavetype.JOURNALFØRING)): List<Long>
    fun ferdigstillOppgave(oppgaveId: Long)
    fun opprettJournalføringsOppgave(journalpostId: JournalpostId, personIdent: Ident, beskrivelse: String, tildeltEnhetsnr: String)
    fun opprettFordelingsOppgave(journalpostId: JournalpostId, personIdent: Ident, beskrivelse: String)
}

enum class Oppgavetype(val verdi: String) {
    JOURNALFØRING("JFR"),
    FORDELING("FDR")
}