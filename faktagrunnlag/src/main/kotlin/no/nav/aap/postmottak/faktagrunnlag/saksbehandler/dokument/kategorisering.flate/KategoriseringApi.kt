package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.komponenter.dbconnect.transaction

fun NormalOpenAPIRoute.kategoriseringApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/kategorisering") {
        get<JournalpostId, KategoriseringGrunnlagDto> { req ->
            val vurdering = dataSource.transaction {
                BehandlingRepositoryImpl(it).hent(req)
            }
            respond(
                KategoriseringGrunnlagDto(
                    vurdering.vurderinger.kategorivurdering
                        ?.avklaring?.let(::KategoriseringVurderingDto),
                    listOf(1, 2)
                )
            )
        }
    }

}
