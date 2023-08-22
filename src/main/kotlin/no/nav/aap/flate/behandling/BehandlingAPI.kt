package no.nav.aap.flate.behandling

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.aap.domene.behandling.BehandlingTjeneste
import java.util.*

fun Routing.behandlingApi() {
    route("/api/behandling") {
        get("/hent/{referanse}") {
            val referanse = call.parameters.getOrFail("referanse")

            val eksternReferanse = UUID.fromString(referanse)

            val behandling = BehandlingTjeneste.hent(eksternReferanse)

            val dto = DetaljertBehandlingDTO(
                behandling.referanse,
                behandling.type.identifikator(),
                behandling.status(),
                behandling.opprettetTidspunkt,
                behandling.avklaringsbehov().map { avklaringsbehov ->
                    AvklaringsbehovDTO(
                        avklaringsbehov.definisjon,
                        avklaringsbehov.status()
                    )
                })

            call.respond(HttpStatusCode.OK, dto)
        }
    }
}