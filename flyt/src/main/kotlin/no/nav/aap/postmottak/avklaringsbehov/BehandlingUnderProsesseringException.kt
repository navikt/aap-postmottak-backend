package no.nav.aap.postmottak.avklaringsbehov

import io.ktor.http.*
import no.nav.aap.postmottak.exception.ErrorRespons
import no.nav.aap.postmottak.exception.FlytOperasjonException

class BehandlingUnderProsesseringException : FlytOperasjonException,
    RuntimeException("Behandlingen har prosesseringsjobber som venter eller har feilet. Vent til disse er ferdig prosesserte") {
    override fun status(): HttpStatusCode {
        return HttpStatusCode.Conflict
    }

    override fun body(): ErrorRespons {
        return ErrorRespons(cause?.message)
    }
}
