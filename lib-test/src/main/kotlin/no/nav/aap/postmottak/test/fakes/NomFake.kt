package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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

            when {
                testPerson == null -> call.respond(HttpStatusCode.NotFound)
                else -> call.respond(testPerson.erSkjermet)
            }
        }
    }

}
