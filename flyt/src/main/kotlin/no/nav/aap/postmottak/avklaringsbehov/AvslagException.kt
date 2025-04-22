package no.nav.aap.postmottak.avklaringsbehov

import io.ktor.http.*
import no.nav.aap.postmottak.exception.ErrorRespons
import no.nav.aap.postmottak.exception.FlytOperasjonException

class AvslagException : FlytOperasjonException,
    RuntimeException("Fikk innkommende dokument på tidligere avslått sak, mangler håndtering av dette") {
    override fun status(): HttpStatusCode {
        return HttpStatusCode.Conflict
    }

    override fun body(): ErrorRespons {
        return ErrorRespons(cause?.message)
    }
}
