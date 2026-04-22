package no.nav.aap.postmottak.api.drift

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.api.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("api.drift")

fun NormalOpenAPIRoute.driftApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
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

        route("/journalpost/{referanse}/info") {
            authorizedGet<JournalpostId, JournalpostDriftsinfoDto>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "referanse",
                    ),
                    operasjon = Operasjon.DRIFTE,
                ),
            ) { params ->
                val dto = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val journalpostId = JournalpostId(params.referanse)

                    val innkommendeJournalpostRepository =
                        repositoryProvider.provide<InnkommendeJournalpostRepository>()
                    val journalpostRepository = repositoryProvider.provide<JournalpostRepository>()
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()

                    val innkommendeJournalpost = innkommendeJournalpostRepository.hentHvisEksisterer(journalpostId)
                    val journalpost = journalpostRepository.hentHvisEksisterer(journalpostId)

                    if (innkommendeJournalpost == null && journalpost == null) {
                        return@transaction null
                    }

                    val behandlinger = behandlingRepository.hentAlleBehandlingerForSak(journalpostId)
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
                        saksnummer = journalpost?.saksnummer,
                        behandlinger = behandlinger,
                    )
                }

                if (dto == null) {
                    respondWithStatus(HttpStatusCode.NotFound)
                } else {
                    krevDtoErUtenFødselsnummer(dto)
                    respond(dto)
                }
            }
        }
    }
}