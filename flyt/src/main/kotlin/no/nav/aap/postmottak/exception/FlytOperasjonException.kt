package no.nav.aap.postmottak.exception

import io.ktor.http.*

interface FlytOperasjonException {
    fun status(): HttpStatusCode

    fun body(): ErrorRespons
}