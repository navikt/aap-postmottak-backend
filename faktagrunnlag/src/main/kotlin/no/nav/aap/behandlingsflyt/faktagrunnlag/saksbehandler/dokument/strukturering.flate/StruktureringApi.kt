package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.strukturering.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.komponenter.dbconnect.transaction

fun NormalOpenAPIRoute.struktureringApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/strukturering") {
        get<JournalpostId, StruktureringVurderingDto> { req ->
            val vurdering = dataSource.transaction {
                BehandlingRepositoryImpl(it).hent(req)
            }
            respond(
                StruktureringVurderingDto(
                    vurdering.vurderinger.struktureringsvurdering?.vurdering,
                    listOf(1,2)
                )
            )
        }
    }

}
