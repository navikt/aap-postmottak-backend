package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategorivurderingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet


fun NormalOpenAPIRoute.kategoriseringApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/kategorisering") {
        authorizedGet<Behandlingsreferanse, KategoriseringGrunnlagDto>(JournalpostPathParam("referanse")) { req ->
            val vurdering = dataSource.transaction(readOnly = true) {
                val behandlingId = BehandlingRepositoryImpl(it).hent(req).id
                KategorivurderingRepository(it).hentKategoriAvklaring(behandlingId)
            }
            respond(
                KategoriseringGrunnlagDto(
                    vurdering?.avklaring?.let(::KategoriseringVurderingDto),
                    listOf(1, 2)
                )
            )
        }
    }

}
