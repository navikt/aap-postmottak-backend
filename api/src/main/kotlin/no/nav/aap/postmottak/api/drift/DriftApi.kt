package no.nav.aap.postmottak.api.drift

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.api.auditlog.DefaultAuditLogConfig
import no.nav.aap.postmottak.api.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.gateway.JournalføringService
import no.nav.aap.postmottak.joarkavstemmer.AvstemmingsrapportRepository
import no.nav.aap.postmottak.joarkavstemmer.Avstemmingsutfall
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.Drift
import no.nav.aap.tilgang.DriftLes
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.RollerConfig
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import java.time.Instant
import java.time.LocalDate
import javax.sql.DataSource

data class AvstemmingsrapportDto(
    val startetTidspunkt: Instant?,
    val fullfortTidspunkt: Instant?,
    val resultater: List<JournalpostAvstemmingsresultatDto>,
)

enum class AvstemmingsutfallDto {
    UAVSTEMT_UKJENT,
    UAVSTEMT_KELVIN,
    GOSYS_OPPGAVE_OPPRETTET,
    FORDELINGSOPPGAVE_OPPRETTET,
    ALLEREDE_HAR_OPPGAVE,
    IKKE_NOK_INFORMASJON,
}

data class JournalpostAvstemmingsresultatDto(
    val journalpostId: Long,
    val utfall: AvstemmingsutfallDto,
    val kanal: String?,
    val datoOpprettet: LocalDate?,
)

private fun NormalOpenAPIRoute.joarkAvstemmingApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
) {
    route("/joark-avstemming") {
        authorizedGet<Unit, AvstemmingsrapportDto>(
            RollerConfig(listOf(DriftLes, Drift)),
        ) {
            val rapport = dataSource.transaction(readOnly = true) { connection ->
                repositoryRegistry.provider(connection).provide<AvstemmingsrapportRepository>().hentSiste()
            }
            respond(
                AvstemmingsrapportDto(
                    startetTidspunkt = rapport?.startetTidspunkt,
                    fullfortTidspunkt = rapport?.fullførtTidspunkt,
                    resultater = rapport?.resultater.orEmpty().map { resultat ->
                        JournalpostAvstemmingsresultatDto(
                            journalpostId = resultat.journalpostId,
                            utfall = when (resultat.utfall) {
                                Avstemmingsutfall.UAVSTEMT_UKJENT -> AvstemmingsutfallDto.UAVSTEMT_UKJENT
                                Avstemmingsutfall.UAVSTEMT_KELVIN -> AvstemmingsutfallDto.UAVSTEMT_KELVIN
                                Avstemmingsutfall.GOSYS_OPPGAVE_OPPRETTET -> AvstemmingsutfallDto.GOSYS_OPPGAVE_OPPRETTET
                                Avstemmingsutfall.FORDELINGSOPPGAVE_OPPRETTET -> AvstemmingsutfallDto.FORDELINGSOPPGAVE_OPPRETTET
                                Avstemmingsutfall.ALLEREDE_HAR_OPPGAVE -> AvstemmingsutfallDto.ALLEREDE_HAR_OPPGAVE
                                Avstemmingsutfall.IKKE_NOK_INFORMASJON -> AvstemmingsutfallDto.IKKE_NOK_INFORMASJON
                            },
                            kanal = resultat.kanal,
                            datoOpprettet = resultat.datoOpprettet,
                        )
                    },
                )
            )
        }
    }
}

fun NormalOpenAPIRoute.driftApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/drift") {
        joarkAvstemmingApi(dataSource, repositoryRegistry)
        route("/sak/{saksnummer}/journalposter") {
            authorizedGet<SaksnummerPathParam, SakJournalposterDto>(
                RollerConfig(listOf(DriftLes, Drift)),
            ) { params ->
                val journalposter = dataSource.transaction(readOnly = true) { connection ->
                    repositoryRegistry.provider(connection).provide<SaksnummerRepository>()
                        .hentJournalposterForSak(params.saksnummer)
                        .map {
                            JournalpostPåSakDto(
                                journalpostId = it.journalpostId,
                                journalstatus = it.journalstatus,
                                mottattDato = it.mottattDato,
                                kanal = it.kanal,
                                tema = it.tema,
                            )
                        }
                }
                respond(SakJournalposterDto(journalposter))
            }
        }

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

        route("/ettersend-for-person") {
            authorizedPost<Unit, EttersendForPersonResponse, EttersendForPersonRequest>(
                AuthorizationBodyPathConfig(operasjon = Operasjon.DRIFTE),
                DefaultAuditLogConfig.fraPersonreferanseBody()
            ) { _, req ->
                val input = req.valider()
                val journalpostId = JournalføringService(gatewayProvider).opprettJournalpostForPerson(
                    ident = Ident(input.ident),
                    pdf = input.pdf,
                    filNavn = input.filNavn,
                    eksternReferanseId = input.eksternReferanseId
                )
                respond(EttersendForPersonResponse(journalpostId.referanse))
            }
        }

        route("/journalpost/{referanse}/kopier-journalpost") {
            authorizedPost<JournalpostId, JournalpostId, Unit>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "referanse",
                    ),
                    operasjon = Operasjon.DRIFTE
                )
            ) { journalpostIdParam, _ ->
                val nyJournalpostId = dataSource.transaction(readOnly = true) { connection ->
                    val journalpostService =
                        JournalpostService.konstruer(repositoryRegistry.provider(connection), gatewayProvider)
                    val journalføringService = JournalføringService(gatewayProvider)
                    val journalpostId = JournalpostId(journalpostIdParam.referanse)
                    val journalpost = journalpostService.hentJournalpost(journalpostId = journalpostId)
                    if (journalpost.tema != "AAP") {
                        throw UgyldigForespørselException("Kan ikke kopiere en journalpost som ikke er på tema AAP")
                    }
                    journalføringService.kopierJournalpost(journalpost)
                }

                respond(nyJournalpostId)
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