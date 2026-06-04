package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.HttpStatusCode
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteUtbetalingerRequest
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

        post("/api/v1/maksdato") {
            val parsedRequest = call.receive<MaksdatoRequest>()
            if (parsedRequest.personidentifikator == TestIdenter.IDENT_MED_SAK_I_ARENA.identifikator) {
                call.respond(
                    """
                    {
                      "sakliste": [
                        {
                          "sakId": 1234,
                          "saknummer": "ABC-123",
                          "sakStatus": "AKTIV",
                          "sakRegistrert": "2024-01-01",
                          "sakAvsluttet": null,
                          "har_11_12_forlengelse": false,
                          "utredesForUfor": false,
                          "ferdigAvklart": false,
                          "lopendeVedtak": true,
                          "sisteVedtak": {
                            "vedtakId": 99,
                            "aktfaseKode": "AKT",
                            "vedtaktypeKode": "TYPE",
                            "fra": "2024-01-01",
                            "til": "2024-12-31",
                            "maxdatoOrdinaer": "2025-01-01",
                            "maxdatoUnntak": null,
                            "maxdatoAap": null
                          }
                        }
                      ]
                    }
                    """.trimIndent()
                )
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post("/api/v1/utbetalinger/siste") {
            val parsedRequest = call.receive<SisteUtbetalingerRequest>()
            if (parsedRequest.personidentifikator == TestIdenter.IDENT_MED_SAK_I_ARENA.identifikator) {
                call.respond("""{"utbetalingsdato": "2024-05-10"}""")
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

    }
}