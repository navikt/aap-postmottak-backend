package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import java.time.LocalDate

fun NormalOpenAPIRoute.finnSakApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/finnSak") {
        get<JournalpostId, FinnSakGrunnlagDto> { req ->
            val response = dataSource.transaction {
                val behandling = BehandlingRepositoryImpl(it).hent(req)
                val saksvurdering = behandling.vurderinger.saksvurdering
                val relaterteSaker = SaksnummerRepository(it).hentSaksnummre(behandling.id)

                FinnSakGrunnlagDto(
                    vurdering = saksvurdering?.let { FinnSakVurderingDto(saksvurdering.toString()) },
                    saksinfo = relaterteSaker.map { SaksInfoDto(it.saksnummer, it.periode) }
                )
            }
            respond(response)
        }
    }

}
