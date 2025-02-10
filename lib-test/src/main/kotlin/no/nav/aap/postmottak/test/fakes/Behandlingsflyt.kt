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

val defaultFinnEllerOpprett: suspend RoutingContext.() -> Unit = {
    call.respond(
        BehandlingsflytSak(
            "123321123",
            Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31)),
        )
    )
}

val defaultFinn: suspend RoutingContext.() -> Unit = {
    val body = DefaultJsonMapper.fromJson<FinnSaker>(call.receiveText())
    if (body.ident == IDENT_UTEN_SAK.identifikator) {
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

val tomFinn: suspend RoutingContext.() -> Unit = {
    call.respond(emptyList<BehandlingsflytSak>())
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
        post("/api/sak/finnEllerOpprett", finnEllerOpprett)

        post("/api/sak/finn", finn)

        post("/api/hendelse/send", send)
    }

}
