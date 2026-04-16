package no.nav.aap.postmottak.test.fakes

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.test.ErrorRespons
import no.nav.aap.postmottak.test.modell.MockUnleashFeature
import no.nav.aap.postmottak.test.modell.MockUnleashFeatures
import no.nav.aap.unleash.PostmottakFeature

fun Application.unleashFake() {
    install(ContentNegotiation) {
        register(
            ContentType.Application.Json,
            JacksonConverter(objectMapper = DefaultJsonMapper.objectMapper(), true)
        )
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            this@unleashFake.log.info("Unleash :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorRespons(cause.message)
            )
        }
    }
    //create route
    routing {
        get("/api/client/features") {
            val features = PostmottakFeature.entries.map { MockUnleashFeature(it.name, true) }
            val response = MockUnleashFeatures(features = features)

            call.respond(response)
        }
    }
}