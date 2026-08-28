package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteUtbetalingerRequest
import no.nav.aap.postmottak.test.modell.TestArenaSak
import no.nav.aap.postmottak.test.modell.TestPersoner

fun Application.arenaoppslagFake() {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    routing {
        post("/api/v1/person/historikk") {
            val parsedRequest = call.receive<HarHistorikkRequest>()

            val testPerson = TestPersoner.hentPerson(parsedRequest.personidentifikator)

            if (testPerson?.harHistorikkIArena == true) {
                call.respond("""{"harHistorikk": true}""")
                return@post
            }
            call.respond("""{"harHistorikk": false}""")
        }

        post("/api/v1/person/historikk/signifikant") {
            val parsedRequest = call.receive<SignifikantHistorikkRequest>()
            val testPerson = TestPersoner.hentPerson(parsedRequest.personidentifikator)
            if (testPerson?.harHistorikkIArena == true) {
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
            val arenaSak = TestPersoner.hentPerson(parsedRequest.personidentifikator)?.arenaSak

            if (arenaSak == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            call.respond(maksdatoRespons(arenaSak))
        }

        post("/api/v1/utbetalinger/siste") {
            val parsedRequest = call.receive<SisteUtbetalingerRequest>()
            val arenaSak = TestPersoner.hentPerson(parsedRequest.personidentifikator)?.arenaSak

            if (arenaSak == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            call.respond("""{"utbetalingsdato": "${arenaSak.sisteUtbetalingsdato}"}""")
        }

    }
}

private fun maksdatoRespons(arenaSak: TestArenaSak): String {
    val sisteVedtak = arenaSak.sisteVedtak
    return """
        {
          "sak": 
            {
              "sakId": ${arenaSak.sakId},
              "saknummer": "${arenaSak.saknummer}",
              "sakStatus": "${arenaSak.sakStatus}",
              "sakRegistrert": "${arenaSak.sakRegistrert}",
              "sakAvsluttet": ${arenaSak.sakAvsluttet?.let { "\"$it\"" } ?: "null"},
              "har_11_12_forlengelse": false,
              "utredesForUfor": false,
              "ferdigAvklart": false,
              "lopendeVedtak": true,
              "sisteVedtak": {
                "vedtakId": ${sisteVedtak.vedtakId},
                "aktfaseKode": "${sisteVedtak.aktfaseKode}",
                "vedtaktypeKode": "${sisteVedtak.vedtaktypeKode}",
                "fra": "${sisteVedtak.fra}",
                "til": ${sisteVedtak.til?.let { "\"$it\"" } ?: "null"},
                "maxdatoOrdinaer": ${sisteVedtak.maxdatoOrdinaer?.let { "\"$it\"" } ?: "null"},
                "maxdatoUnntak": ${sisteVedtak.maxdatoUnntak?.let { "\"$it\"" } ?: "null"},
                "maxdatoAap": ${sisteVedtak.maxdatoAap?.let { "\"$it\"" } ?: "null"}
              }
            }
        }
        """.trimIndent()
}
