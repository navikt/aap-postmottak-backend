package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.gateway.BehandlingsflytSak
import no.nav.aap.postmottak.gateway.Klagebehandling
import no.nav.aap.postmottak.klient.behandlingsflyt.FinnSaker
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

fun Application.behandlingsflytFake() {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }

    routing {
        post("/api/sak/finnEllerOpprett") {
            call.respond(
                BehandlingsflytSak(
                    saksnummer = Saksnummer.valueOf(Random.nextLong(123456)).toString(),
                    periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31)),
                    resultat = null
                ),
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
                                saksnummer = Saksnummer.valueOf(Random.nextLong(123456)).toString(),
                                periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31)),
                                resultat = ResultatKode.TRUKKET
                            )
                        )
                    )
                }

                else -> {
                    call.respond(
                        listOf(
                            BehandlingsflytSak(
                                saksnummer = Saksnummer.valueOf(Random.nextLong(123456)).toString(),
                                periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31)),
                                resultat = null
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
