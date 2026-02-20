package no.nav.aap.postmottak.api.faktagrunnlag.tema

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.api.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import java.net.URI
import javax.sql.DataSource

fun NormalOpenAPIRoute.avklarTemaApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling/{referanse}") {
        route("/grunnlag/avklarTemaVurdering") {
            authorizedGet<BehandlingsreferansePathParam, AvklarTemaGrunnlagDto>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "referanse", journalpostIdFraBehandlingResolver(repositoryRegistry, dataSource)
                    )
                )
            ) { req ->
                val grunnlag = dataSource.transaction(readOnly = true) {
                    val repositoryProvider = repositoryRegistry.provider(it)
                    val behandling = repositoryProvider.provide(BehandlingRepository::class).hent(req)
                    val journalpost =
                        repositoryProvider.provide(JournalpostRepository::class).hentHvisEksisterer(behandling.id)
                    require(journalpost != null) { "Fant ikke journalpost" }
                    val arkivDokumenter = journalpost.finnArkivVarianter()
                    AvklarTemaGrunnlagDto(
                        repositoryProvider.provide(AvklarTemaRepository::class)
                            .hentTemaAvklaring(behandling.id)?.skalTilAap?.let(::AvklarTemaVurderingDto),
                        arkivDokumenter.map { it.dokumentInfoId.dokumentInfoId })
                }
                respond(grunnlag)
            }
        }
        // TODO: Denne skal fjernes etter at frontend redirecter til gosys direkte
        route("/endre-tema") {
            @Suppress("UnauthorizedPost")
            post<BehandlingsreferansePathParam, EndreTemaResponse, Unit> { req, _ ->
                val url = URI.create(requiredConfigForKey("gosys.url"))
                respond(EndreTemaResponse(url.toString()))
            }
        }
    }
}


data class EndreTemaResponse(
    val redirectUrl: String
)
