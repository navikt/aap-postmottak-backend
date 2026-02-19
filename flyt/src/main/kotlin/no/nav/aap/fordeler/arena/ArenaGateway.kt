package no.nav.aap.fordeler.arena

import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

interface ArenaGateway : Gateway {
    fun nyesteAktiveSak(ident: Ident): String?
    fun harAktivSak(ident: Ident): Boolean
    fun opprettArenaOppgave(arenaOpprettetForespørsel: ArenaOpprettOppgaveForespørsel): ArenaOpprettOppgaveRespons
    fun behandleKjoerelisteOgOpprettOppgave(journalpostId: JournalpostId): String
}

data class ArenaOpprettOppgaveRespons(
    val oppgaveId: String,
    val arenaSakId: String?
)

data class ArenaOpprettOppgaveForespørsel(
    val fnr: String,
    val enhet: String,
    val tittel: String,
    val titler: List<String> = emptyList(),
    val oppgaveType: ArenaOppgaveType
)

enum class ArenaOppgaveType(val tekst: String) {
    STARTVEDTAK("Start Vedtaksbehandling - automatisk journalfør"),
    BEHENVPERSON("Behandle henvendelse - Person")
}
