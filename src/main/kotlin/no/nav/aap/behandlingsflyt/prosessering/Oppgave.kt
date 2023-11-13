package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection

abstract class Oppgave {

    abstract fun utfør(connection: DBConnection, input: OppgaveInput)

    abstract fun type(): String

    /**
     * Antall ganger oppgaven prøves før den settes til feilet
     */
    fun retries(): Int {
        return 3
    }

    override fun toString(): String {
        return type()
    }
}
