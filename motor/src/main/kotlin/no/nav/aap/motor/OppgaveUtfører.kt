package no.nav.aap.motor

interface OppgaveUtfører {

    fun utfør(input: OppgaveInput)

    /**
     * Antall ganger oppgaven prøves før den settes til feilet
     */
    fun retries(): Int {
        return 3
    }

}
