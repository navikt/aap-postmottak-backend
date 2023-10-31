package no.nav.aap.behandlingsflyt.prosessering

object OppgaveType {
    private val oppgaver = HashMap<String, Oppgave>()

    init {
        oppgaver[ProsesserBehandlingOppgave.type()] = ProsesserBehandlingOppgave
    }

    fun parse(type: String): Oppgave {
        return oppgaver.getValue(type)
    }

}
