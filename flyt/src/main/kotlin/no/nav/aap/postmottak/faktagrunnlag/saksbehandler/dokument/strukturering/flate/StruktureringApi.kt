package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurderingRepository
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
            val (kategorivurdering, struktureringsvurdering) = dataSource.transaction(readOnly = true) {
                val repositoryProvider = RepositoryProvider(it)
                val behandling = repositoryProvider.provide(BehandlingRepository::class).hent(req)
                val kategorivurdering = repositoryProvider.provide(KategoriVurderingRepository::class).hentKategoriAvklaring(behandling.id)
                val struktureringsvurdering = repositoryProvider.provide(StruktureringsvurderingRepository::class).hentStruktureringsavklaring(behandling.id)
                Pair(kategorivurdering, struktureringsvurdering)
            }

            checkNotNull(kategorivurdering) { "Behandlingen mangler kategorisering" }

            respond(
                StruktureringGrunnlagDto(
                    struktureringsvurdering
                        ?.vurdering?.let(::StruktureringVurderingDto),
                    kategorivurdering.avklaring,
                    listOf(1, 2)
                )
            )
        }
    }

}
