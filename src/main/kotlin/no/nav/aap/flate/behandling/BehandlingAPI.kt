package no.nav.aap.flate.behandling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.throws
import io.ktor.http.*
import no.nav.aap.domene.behandling.BehandlingTjeneste
import java.util.*

fun NormalOpenAPIRoute.behandlingApi() {
    route("/api/behandling") {
        route("/hent/{referanse}")
            .throws(HttpStatusCode.BadRequest, IllegalArgumentException::class) {
                throws(HttpStatusCode.NoContent, NoSuchElementException::class) {
                    get<HentBehandlingDTO, DetaljertBehandlingDTO> { req ->
                        val referanse = req.referanse

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
                                        .map { vp ->
                                            VilkårsperiodeDTO(
                                                periode = vp.periode,
                                                utfall = vp.utfall,
                                                manuellVurdering = vp.manuellVurdering,
                                                begrunnelse = vp.begrunnelse
                                            )
                                        })
                            },
                            aktivtSteg = behandling.stegHistorikk().last().tilstand.steg()
                        )

                        respond(dto)
                    }
                }
            }
    }
}