package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerRequest

fun Application.arenaoppslagFake() {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    routing {
        post("/api/v1/person/eksisterer") {
            val parsedRequest = call.receive<SakerRequest>()
            if (parsedRequest.personidentifikatorer.contains(TestIdenter.IDENT_MED_SAK_I_ARENA.identifikator)) {
                call.respond("""{"eksisterer": true}""")
                return@post
            }
            call.respond("""{"eksisterer": false}""")
        }

        post("/api/v1/person/signifikant-historikk") {
            val parsedRequest = call.receive<SignifikanteSakerRequest>()
            if (parsedRequest.personidentifikatorer.contains(TestIdenter.IDENT_MED_SAK_I_ARENA.identifikator)) {
                call.respond(
                    """
                    {
                      "harSignifikantHistorikk" : true,
                      "signifikanteSaker" : [ "1234" ]
                    }
                    """.trimIndent()
                )
            } else {
                call.respond(
                    """
                    {
                      "harSignifikantHistorikk" : false,
                      "signifikanteSaker" : [ ]
                    }
                    """.trimIndent()
                )
            }
        }

    }
}