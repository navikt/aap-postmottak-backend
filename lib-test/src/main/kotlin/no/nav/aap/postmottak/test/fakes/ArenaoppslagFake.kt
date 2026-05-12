package no.nav.aap.postmottak.test.fakes

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.api.intern.SakerRequest
import no.nav.aap.api.intern.SignifikanteSakerRequest
import no.nav.aap.komponenter.json.DefaultJsonMapper

fun Application.arenaoppslagFake() {
    install(ContentNegotiation) {
        jackson()
    }

    routing {
        post("/api/v1/person/eksisterer") {
            val reqBody = call.receive<SakerRequest>()
            if (reqBody.personidentifikatorer.contains(TestIdenter.IDENT_MED_SAK_I_ARENA.identifikator)) {
                call.respond("""{"eksisterer": true}""")

            }
            call.respond("""{"eksisterer": false}""")
        }

        post("/api/v1/person/signifikant-historikk") {
            val requestBody = call.receiveText()
            val parsed = DefaultJsonMapper.fromJson<SignifikanteSakerRequest>(requestBody)
            if (parsed.personidentifikatorer.contains(TestIdenter.IDENT_MED_SAK_I_ARENA.identifikator)) {
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