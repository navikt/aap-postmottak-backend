package no.nav.aap.postmottak.api.faktagrunnlag.tema

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.api.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.under18.Under18Journalføring
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.avklarTemaApi(
    dataSource: DataSource, repositoryRegistry: RepositoryRegistry, gatewayProvider: GatewayProvider
) {
    route("/api/behandling/{referanse}") {
        route("/grunnlag/avklarTemaVurdering") {
            authorizedGet<BehandlingsreferansePathParam, AvklarTemaGrunnlagDto>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "referanse", journalpostIdFraBehandlingResolver(repositoryRegistry, dataSource)
                    )
                )
            ) { req ->
                val grunnlag = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandling = repositoryProvider.provide(BehandlingRepository::class).hent(req)
                    val journalpost =
                        repositoryProvider.provide(JournalpostRepository::class).hentHvisEksisterer(behandling.id)
                    require(journalpost != null) { "Fant ikke journalpost" }
                    val arkivDokumenter = journalpost.finnArkivVarianter()
                    val saksnummerRepository = repositoryProvider.provide<SaksnummerRepository>()
                    AvklarTemaGrunnlagDto(
                        vurdering = repositoryProvider.provide(AvklarTemaRepository::class)
                            .hentTemaAvklaring(behandling.id)?.let {
                                AvklarTemaVurderingDto(it.skalTilAap, it.tema.name)
                            },
                        dokumenter = arkivDokumenter.map { it.dokumentInfoId.dokumentInfoId },
                        journalpostMetadata = JournalpostMetadata(
                            brevkode = journalpost.hoveddokumentbrevkode,
                            journalfoerendeEnhet = journalpost.journalførendeEnhet
                        ),
                        under18VedBekreftetAap = if (behandling.aktivtSteg() == StegType.AVKLAR_TEMA &&
                            saksnummerRepository.hentSakVurdering(behandling.id) == null
                        ) {
                            Under18Journalføring.konstruer(repositoryProvider, gatewayProvider)
                                .under18VedBekreftetAap(journalpost, saksnummerRepository.hentKelvinSaker(behandling.id).isNotEmpty())
                        } else null,
                    )

                }
                respond(grunnlag)
            }
        }
    }
}