package no.nav.aap.postmottak.api.faktagrunnlag.dokument

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.faktagrunnlag.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.gateway.DokumentOboGateway
import no.nav.aap.postmottak.gateway.JournalpostOboGateway
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.gateway.SafDatoType
import no.nav.aap.postmottak.gateway.SafVariantformat
import no.nav.aap.postmottak.joarkavstemmer.JoarkAvstemmer
import no.nav.aap.postmottak.joarkavstemmer.UavstemtJournalpost
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.flate.DokumentResponsDTO
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.flate.HentDokumentDTO
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.dokumentApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    route("/api/dokumenter") {
        route("/{journalpostId}/{dokumentinfoId}") {
            authorizedGet<HentDokumentDTO, DokumentResponsDTO>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "journalpostId"
                    )
                )
            ) { req ->
                val journalpostId = req.journalpostId
                val dokumentInfoId = req.dokumentinfoId

                val token = token()
                val gateway = gatewayProvider.provide<DokumentOboGateway>()
                val dokumentRespons =
                    gateway.hentDokument(
                        JournalpostId(journalpostId),
                        DokumentInfoId(dokumentInfoId),
                        SafVariantformat.ARKIV.name,
                        currentToken = token
                    )
                pipeline.call.response.headers.append(
                    name = "Content-Disposition", value = "inline; filename=${dokumentRespons.filnavn}"
                )

                respond(DokumentResponsDTO(stream = dokumentRespons.dokument))
            }
        }

        route("/{referanse}/info") {
            authorizedGet<BehandlingsreferansePathParam, DokumentInfoResponsDTO>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "referanse",
                        journalpostIdFraBehandlingResolver(repositoryRegistry, dataSource)
                    )
                )
            ) { req ->
                val journalpostId = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    repositoryProvider.provide<BehandlingRepository>().hent(req).journalpostId
                }

                val journalpost =
                    gatewayProvider.provide(JournalpostOboGateway::class)
                        .hentJournalpost(JournalpostId(journalpostId.referanse), token())
                // TODO: Rydd opp i dette
                val identer =
                    listOfNotNull(journalpost.bruker?.id, journalpost.avsenderMottaker?.id).distinct()
                val personer = gatewayProvider.provide<PersondataGateway>().hentPersonBolk(identer)
                val søker = personer?.getOrDefault(journalpost.bruker?.id, null)
                val avsender = personer?.getOrDefault(journalpost.avsenderMottaker?.id, null)

                respond(
                    DokumentInfoResponsDTO(
                        journalpostId = journalpostId.referanse,
                        søker = DokumentIdent(
                            søker?.ident,
                            søker?.navn?.fulltNavn()
                        ),
                        avsender = DokumentIdent(
                            avsender?.ident,
                            avsender?.navn?.fulltNavn()
                        ),
                        dokumenter = journalpost.dokumenter?.mapNotNull { DokumentDto.fromDokument(it!!) }
                            ?: emptyList(),
                        registrertDato = journalpost.relevanteDatoer?.find { dato ->
                            dato?.datotype == SafDatoType.DATO_REGISTRERT
                        }?.dato?.toLocalDate()
                    )
                )
            }
        }

        route("/finn-ubehandlede") {
            @Suppress("UnauthorizedGet")
            get<Unit, List<UavstemtJournalpost>> {
                val res = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)

                    JoarkAvstemmer(
                        doksikkerhetsnettGateway = gatewayProvider.provide(),
                        regelRepository = repositoryProvider.provide(),
                        behandlingRepository = repositoryProvider.provide(),
                        gosysOppgaveGateway = gatewayProvider.provide(),
                        journalpostGateway = gatewayProvider.provide(),
                        meterRegistry = PrometheusProvider.prometheus
                    ).hentUavstemte()
                }

                respond(res)
            }
        }
    }
}
