package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytSak
import java.time.LocalDate

val defaultFinnEllerOpprett: suspend RoutingContext.() -> Unit = {
        call.respond(
            BehandlingsflytSak(
                (Math.random() * 9999999999).toLong().toString(),
                Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31)),
            )
        )
    }

val defaultFinn: suspend RoutingContext.() -> Unit = {
        call.respond(
            listOf(
                BehandlingsflytSak(
                    (Math.random() * 9999999999).toLong().toString(),
                    Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31)),
                )
            )
        )
}

val defualtSend: suspend RoutingContext.() -> Unit = {

        call.respond(HttpStatusCode.NoContent)

}


fun Application.behandlingsflytFake(
    finnEllerOpprett: suspend RoutingContext.() -> Unit = defaultFinnEllerOpprett,
    finn: suspend RoutingContext.() -> Unit = defaultFinn,
    send: suspend RoutingContext.() -> Unit = defualtSend
) {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }

    routing {
        post("/api/sak/finnEllerOpprett") { finnEllerOpprett() }

        post("/api/sak/finn") { finn() }

        post("/api/soknad/send", send)
    }

}
