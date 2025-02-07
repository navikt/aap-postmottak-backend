package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.veilarbarena() {

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }

    routing {
        post("/veilarbarena/api/v2/arena/hent-status") {
            call.respondText(responsFraArena())
        }
    }

}

private fun responsFraArena(): String {
    return """
            { 
                "oppfolgingsenhet": "9999"
           
            }
    """.trimIndent()
}
