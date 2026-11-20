package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.aap.postmottak.klient.nom.EgenansattRequest
import no.nav.aap.postmottak.test.modell.TestPersoner

fun Application.nomFake() {

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }

    routing {
        post("/skjermet") {
            val personident = call.receive<EgenansattRequest>().personident

            val testPerson = TestPersoner.hentPerson(personident)

            call.respond(testPerson?.erSkjermet ?: false)
        }
    }

}
