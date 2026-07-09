package no.nav.aap.postmottak.api.faktagrunnlag.sak

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.faktagrunnlag.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.VurderOpprettelseAvSakRepository
import no.nav.aap.postmottak.gateway.AapInternApiGateway
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.vurderOpprettelseAvSakApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    route("/api/behandling/{referanse}/grunnlag/vurderOpprettelseAvSak") {
        authorizedGet<BehandlingsreferansePathParam, VurderOpprettelseAvSakGrunnlagDto>(
            AuthorizationParamPathConfig(
                journalpostPathParam = JournalpostPathParam(
                    "referanse",
                    journalpostIdFraBehandlingResolver(repositoryRegistry, dataSource)
                )
            )
        ) { req ->
            val (vurdering, journalpost) = dataSource.transaction(readOnly = true) {
                val repositoryProvider = repositoryRegistry.provider(it)
                val behandling = repositoryProvider.provide<BehandlingRepository>().hent(req)

                val vurdering = repositoryProvider.provide<VurderOpprettelseAvSakRepository>()
                    .hentHvisEksisterer(behandling.id)

                val journalpost = requireNotNull(
                    repositoryProvider.provide<JournalpostRepository>().hentHvisEksisterer(behandling.id)
                ) { "Journalpost ikke funnet. Req: ${req.referanse}." }

                vurdering to journalpost
            }

            // TODO: Returnerer null inntil AapInternApi/arenaoppslag eksponerer dette.
            val arenaSakskontekst = gatewayProvider.provide<AapInternApiGateway>()
                .hentArenaSakskontekst(journalpost.person, journalpost.mottattDato)

            respond(
                VurderOpprettelseAvSakGrunnlagDto(
                    valg = vurdering?.valg,
                    begrunnelse = vurdering?.begrunnelse,
                    arenaSakskontekst = arenaSakskontekst
                )
            )
        }
    }
}

