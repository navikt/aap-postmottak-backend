package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.gateway.BehandlingsflytSak
import no.nav.aap.postmottak.klient.behandlingsflyt.FinnSaker
import java.time.LocalDate

fun Application.behandlingsflytFake(
) {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }

    routing {
        post("/api/sak/finnEllerOpprett") {
            call.respond(
                BehandlingsflytSak(
                    "123321123",
                    Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31)),
                )
            )
        }

        post("/api/sak/finn") {
            val body = DefaultJsonMapper.fromJson<FinnSaker>(call.receiveText())
            if (body.ident == IDENT_UTEN_SAK_I_KELVIN.identifikator) {
                call.respond(emptyList<BehandlingsflytSak>())
            } else {
                call.respond(
                    listOf(
                        BehandlingsflytSak(
                            "123321123",
                            Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31)),
                        )
                    )
                )
            }
        }

        post("/api/hendelse/send") {
            call.respond(HttpStatusCode.NoContent)
        }
    }

}
