package no.nav.aap.postmottak.api.faktagrunnlag.fordeling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.api.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.gateway.ArenasakForManuellVurdering
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.avklarFordelingApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    route("/api/behandling/{referanse}/grunnlag/avklarFordeling") {
        authorizedGet<BehandlingsreferansePathParam, AvklarFordelingGrunnlagDto>(
            AuthorizationParamPathConfig(
                journalpostPathParam = JournalpostPathParam(
                    "referanse", journalpostIdFraBehandlingResolver(repositoryRegistry, dataSource)
                )
            )
        ) { req ->
            val ident = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandling = repositoryProvider.provide(BehandlingRepository::class).hent(req)
                val journalpost =
                    repositoryProvider.provide(JournalpostRepository::class).hentHvisEksisterer(behandling.id)
                requireNotNull(journalpost) { "Fant ikke journalpost" }
                journalpost.person.aktivIdent()
            }

            // NB: Arena-API-et er ikke implementert enda – gatewayen returnerer foreløpig dummy-data.
            val arenasak = runBlocking {
                gatewayProvider.provide(ArenaoppslagGateway::class).hentArenasakForManuellVurdering(ident)
            }

            respond(AvklarFordelingGrunnlagDto(arenasak))
        }
    }
}

data class AvklarFordelingGrunnlagDto(
    val arenasak: ArenasakForManuellVurdering?
)

