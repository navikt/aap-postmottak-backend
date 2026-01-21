package no.nav.aap.postmottak.test.fakes

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.klient.SakerRequest
import no.nav.aap.postmottak.klient.SignifikanteSakerRequest

fun Application.aapInternApiFake() {
    install(ContentNegotiation) {
        jackson()
    }
    routing {
        post("/sakerByFnr") {
            val reqBody = call.receive<SakerRequest>()
            if (reqBody.personidentifikatorer.contains(TestIdenter.IDENT_MED_SAK_I_ARENA.identifikator)) {
                call.respond(
                    """[
                    {
                        "sakId": "1234",
                        "statusKode": "AVSLU",
                        "periode": {
                        "fraOgMedDato": "2020-01-01",
                        "tilOgMedDato": "2020-12-31"
                    },
                        "kilde": "ARENA"
                    }
                ]""".trimIndent()
                )

            }
            call.respond("[]")
        }
        post("/arena/person/aap/eksisterer") {
            val reqBody = call.receive<SakerRequest>()
            if (reqBody.personidentifikatorer.contains(TestIdenter.IDENT_MED_SAK_I_ARENA.identifikator)) {
                call.respond("""{"eksisterer": true}""")

            }
            call.respond("""{"eksisterer": false}""")
        }
        post("/arena/person/aap/signifikant-historikk") {
            val requestBody = call.receiveText()
            val parsed = DefaultJsonMapper.fromJson<SignifikanteSakerRequest>(requestBody)
            if (parsed.personidentifikatorer.contains(TestIdenter.IDENT_MED_SAK_I_ARENA.identifikator)) {
                call.respond("""
                    {
                      "harSignifikantHistorikk" : true,
                      "signifikanteSaker" : [ "1234" ]
                    }
                    """.trimIndent())

            }
            call.respond("""
                    {
                      "harSignifikantHistorikk" : false,
                      "signifikanteSaker" : [ ]
                    }
                    """.trimIndent())
        }

    }
}