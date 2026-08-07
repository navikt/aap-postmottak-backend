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
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteUtbetalingerRequest

fun Application.arenaoppslagFake() {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    // Identer som skal se ut som om de har en (kant-i-kant) sak i Arena, slik at
    // fordelingen havner på manuell vurdering (AVKLAR_FORDELING).
    val identerMedSakIArena = setOf(
        TestIdenter.IDENT_MED_SAK_I_ARENA.identifikator,
        TestIdenter.DEFAULT_IDENT.identifikator, // 21345345210
        TestIdenter.IDENT_MED_KANT_I_KANT_SAK.identifikator,
    )

    routing {
        post("/api/v1/person/historikk") {
            val parsedRequest = call.receive<HarHistorikkRequest>()
            if (parsedRequest.personidentifikator in identerMedSakIArena) {
                call.respond("""{"harHistorikk": true}""")
                return@post
            }
            call.respond("""{"harHistorikk": false}""")
        }

        post("/api/v1/person/historikk/signifikant") {
            val parsedRequest = call.receive<SignifikantHistorikkRequest>()
            if (parsedRequest.personidentifikator in identerMedSakIArena) {
                call.respond(
                    """
                    {
                      "harSignifikantHistorikk" : true,
                      "signifikanteVedtak" : [
                        {
                          "sakId": 1234,
                          "statusKode": "AKTIV",
                          "vedtaktypeKode": null,
                          "fraOgMed": null,
                          "tilDato": null,
                          "rettighetkode": "AAP",
                          "utfallkode": null
                        }
                      ]
                    }
                    """.trimIndent()
                )
            } else {
                call.respond(
                    """
                    {
                      "harSignifikantHistorikk" : false,
                      "signifikanteVedtak" : [ ]
                    }
                    """.trimIndent()
                )
            }
        }

        post("/api/v1/person/maksdato") {
            val parsedRequest = call.receive<MaksdatoRequest>()
            // maxdatoAap er satt "kant-i-kant" (innen 20 uker etter journalpostens mottattDato 2020-12-01)
            // slik at ArenaService.skalManueltFordeles gir true, og fordelingen havner til manuell vurdering.
            if (parsedRequest.personidentifikator == TestIdenter.IDENT_MED_SAK_I_ARENA.identifikator) {
                call.respond(
                    """
                    {
                      "sak": 
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
                            "vedtaktypeKode": "O",
                            "fra": "2024-01-01",
                            "til": "2024-12-31",
                            "maxdatoOrdinaer": "2025-01-01",
                            "maxdatoUnntak": null,
                            "maxdatoAap": "2021-03-01"
                          }
                        }
                    }
                    """.trimIndent()
                )
            } else if (parsedRequest.personidentifikator == TestIdenter.IDENT_MED_KANT_I_KANT_SAK.identifikator) {
                // Journalpostens mottattDato utledes fra relevanteDatoer/DATO_REGISTRERT = 2020-12-01,
                // så maxdatoAap må ligge etter denne og innenfor terskelen på 20 uker (2021-04-20)
                // for at ArenaService.skalManueltFordeles skal gi true for søknadene 134 og 135.
                call.respond(
                    """
                    {
                      "sak": 
                        {
                          "sakId": 1234,
                          "saknummer": "ABC-555",
                          "sakStatus": "AKTIV",
                          "sakRegistrert": "2020-01-01",
                          "sakAvsluttet": null,
                          "har_11_12_forlengelse": false,
                          "utredesForUfor": false,
                          "ferdigAvklart": false,
                          "lopendeVedtak": true,
                          "sisteVedtak": {
                            "vedtakId": 100,
                            "aktfaseKode": "AKT",
                            "vedtaktypeKode": "O",
                            "fra": "2020-01-01",
                            "til": "2021-03-01",
                            "maxdatoOrdinaer": "2021-03-01",
                            "maxdatoUnntak": null,
                            "maxdatoAap": "2021-03-01"
                          }
                        }
                    }
                    """.trimIndent()
                )
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post("/api/v1/utbetalinger/siste") {
            val parsedRequest = call.receive<SisteUtbetalingerRequest>()
            if (parsedRequest.personidentifikator == TestIdenter.IDENT_MED_SAK_I_ARENA.identifikator ||
                parsedRequest.personidentifikator == TestIdenter.IDENT_MED_KANT_I_KANT_SAK.identifikator
            ) {
                call.respond("""{"utbetalingsdato": "2024-05-10"}""")
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

    }
}

