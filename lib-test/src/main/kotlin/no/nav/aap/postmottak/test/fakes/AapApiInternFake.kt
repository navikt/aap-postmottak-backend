package no.nav.aap.postmottak.test.fakes

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.aapInternApiFake(sakerRespons: String = "[]") {
    install(ContentNegotiation) {
        jackson()
    }
    routing {
        post("/sakerByFnr") {
            call.respond(sakerRespons)
        }
    }
}