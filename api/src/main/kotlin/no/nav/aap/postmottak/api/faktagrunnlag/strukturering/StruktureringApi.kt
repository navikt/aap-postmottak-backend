package no.nav.aap.postmottak.api.faktagrunnlag.strukturering


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.struktureringApi(dataSource: DataSource) {
    route("/api/behandling/{referanse}/grunnlag/strukturering") {
        authorizedGet<BehandlingsreferansePathParam, StruktureringGrunnlagDto>(
            AuthorizationParamPathConfig(
                journalpostPathParam = JournalpostPathParam(
                    "referanse",
                    journalpostIdFraBehandlingResolver(dataSource)
                )
            )
        ) { req ->
            val struktureringsvurdering = dataSource.transaction(readOnly = true) {
                val repositoryProvider = RepositoryProvider(it)
                val behandling = repositoryProvider.provide(BehandlingRepository::class).hent(req)
                repositoryProvider.provide(StruktureringsvurderingRepository::class)
                    .hentStruktureringsavklaring(behandling.id)
            }

            respond(
                StruktureringGrunnlagDto(
                    struktureringsvurdering?.let {
                        StruktureringVurderingDto(
                            struktureringsvurdering.kategori,
                            struktureringsvurdering.strukturertDokument
                        )
                    }
                )
            )
        }
    }

}
