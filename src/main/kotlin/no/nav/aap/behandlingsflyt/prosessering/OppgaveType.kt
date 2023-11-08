package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.prosessering.retry.RekjørFeiledeOppgaver

object OppgaveType {
    private val oppgaver = HashMap<String, Oppgave>()

    init {
        oppgaver[ProsesserBehandlingOppgave.type()] = ProsesserBehandlingOppgave
        oppgaver[RekjørFeiledeOppgaver.type()] = RekjørFeiledeOppgaver
    }

    fun parse(type: String): Oppgave {
        return oppgaver.getValue(type)
    }

}
