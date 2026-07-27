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
import no.nav.aap.postmottak.gateway.SykepengerResponse
import no.nav.aap.postmottak.gateway.UtbetaltePerioder
import no.nav.aap.postmottak.test.FakePersoner

data class SykepengerRequest(val personidentifikatorer: Set<String>)

fun Application.sykepengerFake(fakePersoner: FakePersoner) {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    }
    routing {
        post("/utbetalte-perioder-aap") {
            val request = call.receive<SykepengerRequest>()
            val fakePerson = request.personidentifikatorer
                .firstNotNullOfOrNull { fakePersoner.fakePersoner[it] }
            val sykepenger = fakePerson?.sykepenger
            if (sykepenger != null) {
                call.respond(
                    SykepengerResponse(
                        utbetaltePerioder = sykepenger.map {
                            UtbetaltePerioder(
                                fom = it.fom,
                                tom = it.tom,
                                grad = it.grad,
                                organisasjonsnummer = it.organisasjonsnummer
                            )
                        }
                    )
                )
            } else {
                call.respond(SykepengerResponse(emptyList()))
            }
        }
    }
}
