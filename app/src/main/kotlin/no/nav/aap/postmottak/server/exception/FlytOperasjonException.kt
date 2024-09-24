package no.nav.aap.postmottak.server.exception

import io.ktor.http.*
import no.nav.aap.postmottak.ErrorRespons

interface FlytOperasjonException {
    fun status(): HttpStatusCode

    fun body(): ErrorRespons
}