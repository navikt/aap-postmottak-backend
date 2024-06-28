package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import io.ktor.http.*
import no.nav.aap.behandlingsflyt.ErrorRespons
import no.nav.aap.behandlingsflyt.server.exception.FlytOperasjonException

class OutdatedBehandlingException(årsak: String) : FlytOperasjonException, RuntimeException(årsak) {
    override fun status(): HttpStatusCode {
        return HttpStatusCode.Conflict
    }

    override fun body(): ErrorRespons {
        return ErrorRespons(cause?.message)
    }
}
