package no.nav.aap.postmottak.api.faktagrunnlag.strukturering

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.faktagrunnlag.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.digitaliseringApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    route("/api/behandling/{referanse}/grunnlag/digitalisering") {
        authorizedGet<BehandlingsreferansePathParam, DigitaliseringGrunnlagDto>(
            AuthorizationParamPathConfig(
                journalpostPathParam = JournalpostPathParam(
                    "referanse",
                    journalpostIdFraBehandlingResolver(repositoryRegistry, dataSource)
                )
            )
        ) { req ->
            val (journalpost, klagebehandlinger, digitaliseringsvurdering) = dataSource.transaction(readOnly = true) {
                val repositoryProvider = repositoryRegistry.provider(it)
                val behandling = repositoryProvider.provide<BehandlingRepository>().hent(req)

                val digitaliseringsvurdering = repositoryProvider.provide<DigitaliseringsvurderingRepository>()
                    .hentHvisEksisterer(behandling.id)

                val journalpost = repositoryProvider.provide<JournalpostRepository>().hentHvisEksisterer(req)

                requireNotNull(journalpost) { "Journalpost ikke funnet" }

                val saksnummer = repositoryProvider.provide(SaksnummerRepository::class)
                    .hentSakVurdering(behandling.id)?.saksnummer

                requireNotNull(saksnummer) { "Kun journalposter som er journalført på Kelvin-sak skal digitaliseres" }
                val klagebehandlinger = gatewayProvider.provide<BehandlingsflytGateway>()
                    .finnKlagebehandlinger(saksnummer = Saksnummer(saksnummer))

                Triple(journalpost, klagebehandlinger, digitaliseringsvurdering)
            }

            respond(
                DigitaliseringGrunnlagDto(
                    klagebehandlinger = klagebehandlinger,
                    erPapir = journalpost.erPapir(),
                    vurdering = digitaliseringsvurdering?.let {
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
