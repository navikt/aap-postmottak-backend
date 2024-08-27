package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse

fun NormalOpenAPIRoute.dokumentApi(dataSource: HikariDataSource) {
    route("/api/dokument") {
        route("/{referanse}/") {
            get<BehandlingReferanse, DokumentBehandlingDto> { req ->

                TODO("returner dokumentbehandling ")

                respond(DokumentBehandlingDto())
            }
        }
    }
}
