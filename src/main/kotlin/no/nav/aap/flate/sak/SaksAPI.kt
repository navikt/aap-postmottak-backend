package no.nav.aap.flate.sak

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.person.Personlager
import no.nav.aap.domene.sak.Sakslager
import no.nav.aap.domene.typer.Ident
import no.nav.aap.domene.typer.Saksnummer

fun Routing.saksApi() {
    route("/api/sak") {
        post("/finn") {
            val dto = call.receive<FinnSakForIdentDTO>()

            val ident = Ident(dto.ident)
            val person = Personlager.finn(ident)

            if (person == null) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                val saker = Sakslager.finnSakerFor(person)
                    .map { sak -> SaksinfoDTO(saksnummer = sak.saksnummer.toString(), periode = sak.rettighetsperiode) }

                call.respond(HttpStatusCode.OK, saker)
            }
        }
        get("/hent/{saksnummer}") {
            val saksnummer = call.parameters.getOrFail("saksnummer")

            val sak = Sakslager.hent(saksnummer = Saksnummer(saksnummer))

            val behandlinger = BehandlingTjeneste.hentAlleFor(sak.id).map { behandling ->
                BehandlinginfoDTO(
                    referanse = behandling.referanse,
                    type = behandling.type.identifikator(),
                    status = behandling.status(),
                    opprettet = behandling.opprettetTidspunkt
                )
            }

            call.respond(HttpStatusCode.OK, behandlinger)
        }
    }
}
