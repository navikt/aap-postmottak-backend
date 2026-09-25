package no.nav.aap.postmottak.api.drift

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import io.ktor.server.config.configLoaders
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.api.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.faktagrunnlag.register.PersonService
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.gateway.JournalføringService
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import javax.sql.DataSource

fun NormalOpenAPIRoute.driftApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/drift") {
        route("/behandling/{referanse}/prosesser") {
            authorizedPost<BehandlingsreferansePathParam, Unit, Unit>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "referanse",
                        journalpostIdFraBehandlingResolver(repositoryRegistry, dataSource)
                    ),
                    operasjon = Operasjon.DRIFTE
                )
            ) { params, _ ->
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val behandling = behandlingRepository.hent(Behandlingsreferanse(params.referanse))

                    FlytJobbRepository(connection).leggTil(
                        JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(
                            behandling.journalpostId.referanse, behandling.id.id
                        ).medCallId()
                    )
                }
                respondWithStatus(HttpStatusCode.NoContent)
            }
        }

        route("/person/journalposter/søk") {
            authorizedPost<Unit, PersonSøkDriftsinfoDto, IdentDto>(
                AuthorizationBodyPathConfig(
                    operasjon = Operasjon.DRIFT_LES
                )
            ) { _, req ->
                val journalposter = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val innkommendeJournalpostRepository =
                        repositoryProvider.provide<InnkommendeJournalpostRepository>()

                    innkommendeJournalpostRepository.finn(Ident(req.ident))
                }

                respond(PersonSøkDriftsinfoDto(journalposter.map { InnkommendeJournalpostDto.fraDomene(it) }))
            }
        }

        route("/journalpost/{journalpostId}/kopier-journalpost") {
            authorizedPost<JournalpostPathParam, JournalpostId, Unit>(
                AuthorizationBodyPathConfig(
                    operasjon = Operasjon.DRIFTE
                )
            ) { journalpostIdParam, _ ->
                val nyJournalpostId = dataSource.transaction(readOnly = true) { connection ->
                    val journalpostService = JournalpostService.konstruer(repositoryRegistry.provider(connection), gatewayProvider)
                    val journalføringService = JournalføringService(gatewayProvider)
                    val journalpostId = JournalpostId(journalpostIdParam.param.toLong())
                    val journalpost = journalpostService.hentJournalpost(journalpostId = journalpostId)
                    if (journalpost.tema != "AAP") {
                        throw UgyldigForespørselException("Kan ikke kopiere en journalpost som ikke er på tema AAP")
                    }
                    journalføringService.kopierJournalpost(journalpost)
                }

                respond(JournalpostId(nyJournalpostId.kopierJournalpostId.toLong()))
            }
        }

        route("/journalpost/{referanse}/info") {
            authorizedGet<JournalpostId, JournalpostDriftsinfoDto>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "referanse",
                    ),
                    operasjon = Operasjon.DRIFT_LES,
                ),
            ) { params ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val journalpostId = JournalpostId(params.referanse)

                    val innkommendeJournalpostRepository =
                        repositoryProvider.provide<InnkommendeJournalpostRepository>()
                    val journalpostRepository = repositoryProvider.provide<JournalpostRepository>()
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val avklaringsbehovRepository by lazy { repositoryProvider.provide<AvklaringsbehovRepository>() }
                    val saksnummerRepository by lazy { repositoryProvider.provide<SaksnummerRepository>() }

                    val innkommendeJournalpost = innkommendeJournalpostRepository.hentHvisEksisterer(journalpostId)
                    val journalpost = journalpostRepository.hentHvisEksisterer(journalpostId)

                    val behandlinger = behandlingRepository.hentAlleBehandlingerForJournalpost(journalpostId)
                        .map { behandling ->
                            val avklaringsbehovene = avklaringsbehovRepository
                                .hentAvklaringsbehovene(behandling.id)
                                .alle()
                                .flatMap { avklaringsbehov ->
                                    avklaringsbehov.historikk.map { endring ->
                                        ForenkletAvklaringsbehov(
                                            definisjon = avklaringsbehov.definisjon,
                                            status = endring.status,
                                            årsakTilSettPåVent = endring.grunn,
                                            tidsstempel = endring.tidsstempel,
                                            endretAv = endring.endretAv
                                        )
                                    }
                                }.sortedByDescending { it.tidsstempel }

                            BehandlingDriftsinfo.fra(behandling, avklaringsbehovene)
                        }
                        .sortedByDescending { it.opprettet }

                    JournalpostDriftsinfoDto(
                        innkommendeStatus = innkommendeJournalpost?.status,
                        brevkode = innkommendeJournalpost?.brevkode,
                        tema = journalpost?.tema,
                        fordelingsresultat = innkommendeJournalpost?.regelresultat,
                        journalstatus = journalpost?.status,
                        mottattDato = journalpost?.mottattDato,
                        kanal = journalpost?.kanal,
                        saksnummer = journalpost?.saksnummer
                            ?: saksnummerRepository.hentSaksnummerForJournalpost(journalpostId),
                        behandlinger = behandlinger,
                    )
                }

                krevDtoErUtenFødselsnummer(dto)
                respond(dto)
            }
        }
    }
}