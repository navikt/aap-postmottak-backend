package no.nav.aap.postmottak.api.faktagrunnlag.sak


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
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
            val response = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = RepositoryProvider(connection)
                val saksnummerRepository = repositoryProvider.provide(SaksnummerRepository::class)
                val behandling = repositoryProvider.provide(BehandlingRepository::class).hent(req)

                val saksvurdering = saksnummerRepository.hentSakVurdering(behandling.id)
                val relaterteSaker = saksnummerRepository.hentKelvinSaker(behandling.id)

                val journalpost = repositoryProvider.provide(JournalpostRepository::class).hentHvisEksisterer(req)
                requireNotNull(journalpost) { "Journalpost ikke funnet" }

                AvklarSakGrunnlagDto(
                    vurdering = saksvurdering?.let { AvklarSakVurderingDto.toDto(saksvurdering) },
                    saksinfo = relaterteSaker.map { SaksInfoDto(it.saksnummer, it.periode) },
                    brevkode = journalpost.hoveddokumentbrevkode
                )
            }
            respond(response)
        }
    }

}
