package no.nav.aap.postmottak.api.faktagrunnlag.strukturering


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.digitaliseringApi(dataSource: DataSource) {
    route("/api/behandling/{referanse}/grunnlag/digitalisering") {
        authorizedGet<BehandlingsreferansePathParam, DigitaliseringGrunnlagDto>(
            AuthorizationParamPathConfig(
                journalpostPathParam = JournalpostPathParam(
                    "referanse",
                    journalpostIdFraBehandlingResolver(dataSource)
                )
            )
        ) { req ->
            val digitaliseringsvurdering = dataSource.transaction(readOnly = true) {
                val repositoryProvider = RepositoryProvider(it)
                val behandling = repositoryProvider.provide(BehandlingRepository::class).hent(req)
                repositoryProvider.provide(DigitaliseringsvurderingRepository::class)
                    .hentHvisEksisterer(behandling.id)
            }
            val journalpost = dataSource.transaction(readOnly = true) {
                RepositoryProvider(it).provide(JournalpostRepository::class).hentHvisEksisterer(req)
            }
            requireNotNull(journalpost) { "Journalpost ikke funnet" }

            respond(
                DigitaliseringGrunnlagDto(
                    journalpost.erPapir(),
                    digitaliseringsvurdering?.let {
                        DigitaliseringvurderingDto(
                            digitaliseringsvurdering.kategori,
                            digitaliseringsvurdering.strukturertDokument,
                            digitaliseringsvurdering.søknadsdato
                        )
                    }
                )
            )
        }
    }

}
