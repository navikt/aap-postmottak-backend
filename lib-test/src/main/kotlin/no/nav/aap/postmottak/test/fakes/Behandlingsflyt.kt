package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.gateway.BehandlingsflytSak
import no.nav.aap.postmottak.gateway.Klagebehandling
import no.nav.aap.postmottak.klient.behandlingsflyt.FinnSaker
import java.time.LocalDate
import java.util.*

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
                    Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31)), null, true
                )
            )
        }

        post("/api/sak/ekstern/finn") {
            val body = DefaultJsonMapper.fromJson<FinnSaker>(call.receiveText())
            when (body.ident) {
                TestIdenter.IDENT_UTEN_SAK_I_KELVIN.identifikator -> {
                    call.respond(emptyList<BehandlingsflytSak>())
                }

                TestIdenter.IDENT_MED_TRUKKET_SAK_I_KELVIN.identifikator -> {
                    call.respond(
                        listOf(
                            BehandlingsflytSak(
                                "123321123",
                                Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31)), ResultatKode.TRUKKET, false
                            )
                        )
                    )
                }

                else -> {
                    call.respond(
                        listOf(
                            BehandlingsflytSak(
                                "123321123",
                                Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31)), null, false
                            )
                        )
                    )
                }
            }
        }

        post("/api/hendelse/send") {
            call.respond(HttpStatusCode.NoContent)
        }

        post("/api/sak/{saksnummer}/finnBehandlingerAvType") {
            call.respond(
                listOf(
                    Klagebehandling(
                        behandlingsReferanse = UUID.randomUUID(),
                        opprettetDato = LocalDate.of(2025, 5, 1)
                    ),
                    Klagebehandling(
                        behandlingsReferanse = UUID.randomUUID(),
                        opprettetDato = LocalDate.of(2025, 6, 20)
                    )
                )
            )
        }
    }

}
