package no.nav.aap.postmottak.avklaringsbehov

import io.ktor.http.*
import no.nav.aap.postmottak.exception.ErrorRespons
import no.nav.aap.postmottak.exception.FlytOperasjonException

class OutdatedBehandlingException(årsak: String) : FlytOperasjonException, RuntimeException(årsak) {
    override fun status(): HttpStatusCode {
        return HttpStatusCode.Conflict
    }

    override fun body(): ErrorRespons {
        return ErrorRespons(cause?.message)
    }
}
