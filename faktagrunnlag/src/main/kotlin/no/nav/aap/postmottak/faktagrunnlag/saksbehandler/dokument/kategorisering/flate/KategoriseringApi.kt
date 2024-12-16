package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategorivurderingRepository
import no.nav.aap.postmottak.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource


fun NormalOpenAPIRoute.kategoriseringApi(dataSource: DataSource) {
    route("/api/behandling/{referanse}/grunnlag/kategorisering") {
        authorizedGet<BehandlingsreferansePathParam, KategoriseringGrunnlagDto>(
            AuthorizationParamPathConfig(
                journalpostPathParam = JournalpostPathParam(
                    "referanse",
                    journalpostIdFraBehandlingResolver(dataSource)
                )
            )
        ) { req ->
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
