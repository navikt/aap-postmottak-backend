package no.nav.aap.postmottak.api.faktagrunnlag.overlevering

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurderingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.overleveringApi(dataSource: DataSource) {
    route("/api/behandling/{referanse}/grunnlag/overlevering") {
        authorizedGet<BehandlingsreferansePathParam, OverleveringGrunnlagDto>(
            AuthorizationParamPathConfig(
                journalpostPathParam = JournalpostPathParam(
                    "referanse",
                    journalpostIdFraBehandlingResolver(dataSource)
                )
            )
        ) { req ->
            val vurdering = dataSource.transaction(readOnly = true) {
                val repositoryProvider = RepositoryProvider(it)
                val behandlingId = repositoryProvider.provide(BehandlingRepository::class).hent(req).id
                repositoryProvider.provide(OverleveringVurderingRepository::class).hentHvisEksisterer(behandlingId)
            }
            respond(
                OverleveringGrunnlagDto(
                    vurdering?.skalOverleveresTilKelvin?.let(::OverleveringVurderingDto)
                )
            )
        }
    }

}