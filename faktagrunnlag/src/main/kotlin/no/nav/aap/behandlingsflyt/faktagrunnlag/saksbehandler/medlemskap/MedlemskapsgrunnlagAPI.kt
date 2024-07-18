package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.medlemskap

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter.MedlemskapGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl

fun NormalOpenAPIRoute.medlemskapsgrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/medlemskap") {
            get<BehandlingReferanse, MedlemskapGrunnlagDto> { req ->
                val medlemskap = dataSource.transaction {
                    val behandling = BehandlingReferanseService(BehandlingRepositoryImpl(it)).behandling(req)
                    val sakRepository = SakRepositoryImpl(it)
                    val person = sakRepository.hent(behandling.sakId).person
                    MedlemskapGateway.innhent(person)
                }

                respond(MedlemskapGrunnlagDto(medlemskap = medlemskap))
            }
        }
    }
}