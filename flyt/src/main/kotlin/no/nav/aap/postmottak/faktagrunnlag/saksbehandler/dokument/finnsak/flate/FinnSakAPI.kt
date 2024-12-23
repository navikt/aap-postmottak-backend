package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.finnSakApi(dataSource: DataSource) {
    route("/api/behandling/{referanse}/grunnlag/finnSak") {
        authorizedGet<BehandlingsreferansePathParam, AvklarSakGrunnlagDto>(
            AuthorizationParamPathConfig(
                journalpostPathParam = JournalpostPathParam(
                    "referanse",
                    journalpostIdFraBehandlingResolver(dataSource)
                )
            )
        ) { req ->
            val response = dataSource.transaction(readOnly = true) {
                val repositoryProvider = RepositoryProvider(it)
                val saksnummerRepository = repositoryProvider.provide(SaksnummerRepository::class)
                val behandling = repositoryProvider.provide(BehandlingRepository::class).hent(req)
                val saksvurdering = saksnummerRepository.hentSakVurdering(behandling.id)
                val relaterteSaker = saksnummerRepository.hentSaksnumre(behandling.id)

                AvklarSakGrunnlagDto(
                    vurdering = saksvurdering?.let { AvklarSakVurderingDto.toDto(saksvurdering) },
                    saksinfo = relaterteSaker.map { SaksInfoDto(it.saksnummer, it.periode) }
                )
            }
            respond(response)
        }
    }

}
