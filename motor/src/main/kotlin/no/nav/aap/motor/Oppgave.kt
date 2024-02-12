package no.nav.aap.motor

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

interface Oppgave {

    fun konstruer(connection: DBConnection): OppgaveUtfører

    fun type(): String

    /**
     * Antall ganger oppgaven prøves før den settes til feilet
     */
    fun retries(): Int {
        return 3
    }
}
