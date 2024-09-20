package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.finnsak.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.komponenter.dbconnect.transaction

fun NormalOpenAPIRoute.finnSakApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/kategorisering") {
        get<JournalpostId, FinnSakGrunnlagDto> { req ->
            val saksnummer = dataSource.transaction {
                BehandlingRepositoryImpl(it).hent(req).saksnummer
            }
            respond(
                FinnSakGrunnlagDto(
                    vurdering = saksnummer?.let { FinnSakVurderingDto(saksnummer.toString()) }
                )
            )
        }
    }

}
