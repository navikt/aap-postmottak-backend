package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.finnsak.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

fun NormalOpenAPIRoute.finnSakApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/finnSak") {
        get<JournalpostId, FinnSakGrunnlagDto> { req ->
            val saksnummer = dataSource.transaction {
                BehandlingRepositoryImpl(it).hent(req).vurderinger.saksvurdering
            }
            respond(
                FinnSakGrunnlagDto(
                    vurdering = saksnummer?.let { FinnSakVurderingDto(saksnummer.toString()) },
                    saksinfo = listOf(
                        SaksInfoDto("Sak #1", Periode(LocalDate.now(), LocalDate.now())),
                        SaksInfoDto("Sak #2", Periode(LocalDate.now(), LocalDate.now())),
                    )
                )
            )
        }
    }

}
