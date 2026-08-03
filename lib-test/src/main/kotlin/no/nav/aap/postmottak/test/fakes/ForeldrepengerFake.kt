package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.postmottak.gateway.Anvist
import no.nav.aap.postmottak.gateway.ForeldrepengerRequest
import no.nav.aap.postmottak.gateway.Utbetalingsgrad
import no.nav.aap.postmottak.gateway.Ytelse
import no.nav.aap.postmottak.gateway.Ytelser
import no.nav.aap.postmottak.test.FakePersoner
import java.time.LocalDate

fun Application.foreldrepengerFake(fakePersoner: FakePersoner) {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    }
    routing {
        post("/hent-ytelse-vedtak") {
            val request = call.receive<ForeldrepengerRequest>()
            val fakePerson = fakePersoner.fakePersoner[request.ident.verdi]
            val ytelser = fakePerson?.foreldrepenger.orEmpty().map {
                Ytelse(
                    ytelse = Ytelser.FORELDREPENGER,
                    saksnummer = "FP-SAK",
                    kildesystem = "FPSAK",
                    ytelseStatus = "AVSLUTTET",
                    vedtattTidspunkt = LocalDate.now(),
                    anvist = listOf(
                        Anvist(
                            periode = it.periode,
                            utbetalingsgrad = Utbetalingsgrad(it.grad),
                            beløp = null
                        )
                    )
                )
            }
            call.respond(ytelser)
        }
    }
}

