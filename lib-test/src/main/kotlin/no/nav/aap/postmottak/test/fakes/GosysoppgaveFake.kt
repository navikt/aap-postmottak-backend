package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.postmottak.klient.gosysoppgave.FinnOppgaverResponse

val getOppgaverDefault: suspend RoutingContext.() -> Unit = {
    call.respond(FinnOppgaverResponse(emptyList()))
}

val postOppgaveDefault: suspend RoutingContext.() -> Unit = {
    call.respond(false)
}

val patchOppgaveDefault: suspend RoutingContext.() -> Unit = {
    call.respond(HttpStatusCode.OK)
}

fun Application.gosysOppgaveFake(
    getOppgaver: suspend RoutingContext.() -> Unit = getOppgaverDefault,
    postOppgave: suspend RoutingContext.() -> Unit = postOppgaveDefault,
    patchOppgave: suspend RoutingContext.() -> Unit = patchOppgaveDefault,
) {

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }

    routing {
        get("/api/v1/oppgaver", getOppgaver)
        post("/api/v1/oppgaver", postOppgave)
        patch("/api/v1/oppgaver/{journalpostId}", patchOppgave)
    }

}
