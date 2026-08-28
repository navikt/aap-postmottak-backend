package no.nav.aap.postmottak.test

import no.nav.aap.fordeler.arena.ArenaOpprettOppgaveForespørsel
import no.nav.aap.postmottak.journalpostogbehandling.Ident

object FssOppgaver {
    val oppgaver: MutableMap<Ident, List<ArenaOpprettOppgaveForespørsel>> = mutableMapOf()
}