package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val finnEnhetBestMatch: suspend RoutingContext.() -> Unit = {
    call.respond("""[{"enhetNr": "superNav!"}]""")
}

val finnEnhet: suspend RoutingContext.() -> Unit = { 
    call.respond("""[{"enhetNr": "9999"}]""")
}

fun Application.norgFake(
    bestMatch: suspend RoutingContext.() -> Unit = finnEnhetBestMatch,
    enhet: suspend RoutingContext.() -> Unit = finnEnhet,
) {

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }

    routing {
        post("/norg2/api/v1/arbeidsfordeling/enheter/bestmatch", bestMatch)
        get("/norg2/api/v1/enhet", enhet)
    }

}
