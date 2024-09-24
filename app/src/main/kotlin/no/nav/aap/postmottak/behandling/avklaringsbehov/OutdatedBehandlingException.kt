package no.nav.aap.postmottak.behandling.avklaringsbehov

import io.ktor.http.*
import no.nav.aap.postmottak.ErrorRespons
import no.nav.aap.postmottak.server.exception.FlytOperasjonException

class OutdatedBehandlingException(årsak: String) : FlytOperasjonException, RuntimeException(årsak) {
    override fun status(): HttpStatusCode {
        return HttpStatusCode.Conflict
    }

    override fun body(): ErrorRespons {
        return ErrorRespons(cause?.message)
    }
}
