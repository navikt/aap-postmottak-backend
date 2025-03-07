package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.postmottak.klient.gosysoppgave.FinnOppgaverResponse
import no.nav.aap.postmottak.klient.gosysoppgave.Oppgave

class ShouldNotBeCalledException(message: String = "This endpoint should not have been called") : Exception(message)

fun Application.gosysOppgaveFake(
) {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }

    routing {
        get("/api/v1/oppgaver") {
            if (call.parameters["journalpostId"] == MED_GOSYS_OPPGAVER.referanse.toString()) {
                call.respond(FinnOppgaverResponse(listOf(Oppgave(1))))
            }
            call.respond(FinnOppgaverResponse(emptyList()))
        }
        post("/api/v1/oppgaver") {
            if (call.parameters["journalpostId"] == MED_GOSYS_OPPGAVER.referanse.toString()) {
                throw ShouldNotBeCalledException("Dette endepunktet skal ikke ha blitt kalt ettersom det alt finnes en oppgave")
            }
            call.respond(false)
        }
        patch("/api/v1/oppgaver/{journalpostId}") {
            call.respond(HttpStatusCode.OK)
        }
    }

}
