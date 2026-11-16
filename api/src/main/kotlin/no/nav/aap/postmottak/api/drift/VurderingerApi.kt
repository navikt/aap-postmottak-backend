package no.nav.aap.postmottak.api.drift

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.fordeler.arena.AvklarFordelingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.vurderingerApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/journalpost/{referanse}/vurderinger") {
        authorizedGet<JournalpostId, VurderingerDriftsinfo>(
            AuthorizationParamPathConfig(
                journalpostPathParam = JournalpostPathParam(
                    "referanse",
                ),
                operasjon = Operasjon.DRIFT_LES,
            ),
        ) { journalpostId ->
            val vurderinger = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandling = repositoryProvider.provide<BehandlingRepository>().hentSisteBehandling(journalpostId)

                VurderingerService(
                    repositoryProvider.provide<AvklarFordelingRepository>(),
                    repositoryProvider.provide<AvklarTemaRepository>(),
                    repositoryProvider.provide<SaksnummerRepository>(),
                    repositoryProvider.provide<DigitaliseringsvurderingRepository>(),
                    repositoryProvider.provide<OverleveringVurderingRepository>(),
                ).hent(behandling.id)
            }
            respond(vurderinger)
        }
    }

}
