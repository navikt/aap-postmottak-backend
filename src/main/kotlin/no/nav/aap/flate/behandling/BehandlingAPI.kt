package no.nav.aap.flate.behandling

import io.github.smiley4.ktorswaggerui.dsl.get
import io.github.smiley4.ktorswaggerui.dsl.route
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.aap.domene.behandling.BehandlingTjeneste
import java.util.*

fun Routing.behandlingApi() {
    route("/api/behandling", {
        tags = listOf("behandling")
    }) {
        get("/hent/{referanse}", {
            request { pathParameter<UUID>("referanse") }
            response {
                HttpStatusCode.OK to {
                    description = "Successful Request"
                    body<DetaljertBehandlingDTO> { }
                }
            }
        }) {
            val referanse = call.parameters.getOrFail("referanse")

            val eksternReferanse = UUID.fromString(referanse)
            val behandling = BehandlingTjeneste.hent(eksternReferanse)

            val dto = DetaljertBehandlingDTO(
                referanse = behandling.referanse,
                type = behandling.type.identifikator(),
                status = behandling.status(),
                opprettet = behandling.opprettetTidspunkt,
                avklaringsbehov = behandling.avklaringsbehov().map { avklaringsbehov ->
                    AvklaringsbehovDTO(
                        definisjon = avklaringsbehov.definisjon,
                        status = avklaringsbehov.status(),
                        endringer = avklaringsbehov.historikk.map { endring ->
                            EndringDTO(
                                status = endring.status,
                                tidsstempel = endring.tidsstempel,
                                begrunnelse = endring.begrunnelse,
                                endretAv = endring.endretAv
                            )
                        }
                    )
                },
                vilkår = behandling.vilkårsresultat().alle().map { vilkår ->
                    VilkårDTO(
                        vilkårstype = vilkår.type,
                        perioder = vilkår.vilkårsperioder()
                            .map { vp -> VilkårsperiodeDTO(periode = vp.periode, utfall = vp.utfall) })
                },
                aktivtSteg = behandling.stegHistorikk().last().tilstand.steg()
            )

            call.respond(HttpStatusCode.OK, dto)
        }
    }
}