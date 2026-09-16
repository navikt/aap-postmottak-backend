package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.gateway.BehandlingsflytSak
import no.nav.aap.postmottak.gateway.Klagebehandling
import no.nav.aap.postmottak.klient.behandlingsflyt.FinnEllerOpprettSak
import no.nav.aap.postmottak.klient.behandlingsflyt.FinnSaker
import no.nav.aap.postmottak.test.modell.TestPersoner
import java.time.LocalDate
import java.util.*
import kotlin.random.Random

fun Application.behandlingsflytFake() {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }

    routing {
        post("/api/sak/finnEllerOpprett") {
            val received = call.receive<FinnEllerOpprettSak>()

            val testperson = TestPersoner.hentPerson(received.ident)

            if (!testperson?.kelvinsaker.isNullOrEmpty()) {
                call.respond(
                    HttpStatusCode.OK,
                    BehandlingsflytSak(
                        saksnummer = testperson.kelvinsaker.first().saksnummer,
                        periode = testperson.kelvinsaker.first().periode,
                        resultat = testperson.kelvinsaker.first().resultat,
                    ),
                )
                return@post
            }

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
            val testperson = TestPersoner.hentPerson(body.ident)

            if (testperson != null) {
                call.respond(testperson.kelvinsaker.map {
                    BehandlingsflytSak(
                        saksnummer = it.saksnummer,
                        periode = it.periode,
                        resultat = it.resultat
                    )
                })
                return@post
            }

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
