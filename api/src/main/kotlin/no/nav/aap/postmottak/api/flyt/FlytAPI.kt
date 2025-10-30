package no.nav.aap.postmottak.api.flyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbStatus
import no.nav.aap.motor.api.JobbInfoDto
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.avklaringsbehov.BehandlingTilstandValidator
import no.nav.aap.postmottak.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.postmottak.faktagrunnlag.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.flyt.flate.visning.DynamiskStegGruppeVisningService
import no.nav.aap.postmottak.flyt.flate.visning.ProsesseringStatus
import no.nav.aap.postmottak.flyt.flate.visning.Visning
import no.nav.aap.postmottak.flyt.utledType
import no.nav.aap.postmottak.hendelse.mottak.BehandlingHendelseHåndterer
import no.nav.aap.postmottak.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.journalpostogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.postmottak.journalpostogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.MDC
import javax.sql.DataSource

fun NormalOpenAPIRoute.flytApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    route("/api/behandling") {
        route("/{referanse}/flyt") {
            authorizedGet<BehandlingsreferansePathParam, BehandlingFlytOgTilstandDto>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "referanse",
                        journalpostIdFraBehandlingResolver(
                            dataSource = dataSource,
                            repositoryRegistry = repositoryRegistry
                        )
                    )
                )
            ) { req ->
                val dto = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository: BehandlingRepository = repositoryProvider.provide()
                    val avklaringsbehovRepository: AvklaringsbehovRepository = repositoryProvider.provide()

                    var behandling = behandlingRepository.hent(req)
                    val flytJobbRepository = FlytJobbRepository(connection)
                    val gruppeVisningService = DynamiskStegGruppeVisningService(repositoryProvider)
                    val jobber = flytJobbRepository.hentJobberForBehandling(behandling.id.toLong())
                        .filter { it.type() == ProsesserBehandlingJobbUtfører.type }


                    // ved avsluttet journalføringsbehandling, finn id til dokumenthåndteringsbehandlingen som hører til samme journalpost
                    val dokumentHåndteringBehnandlingId =
                        if (behandling.typeBehandling == TypeBehandling.Journalføring && behandling.status() == Status.AVSLUTTET) {
                            behandlingRepository.hentAlleBehandlingerForSak(behandling.journalpostId)
                                .find { it.typeBehandling == TypeBehandling.DokumentHåndtering }
                                ?.referanse?.referanse
                        } else null
                    val prosessering =
                        Prosessering(
                            utledStatus(jobber),
                            jobber.map {
                                JobbInfoDto(
                                    id = it.jobbId(),
                                    type = it.type(),
                                    status = it.status(),
                                    planlagtKjøretidspunkt = it.nesteKjøring(),
                                    metadata = mapOf(),
                                    antallFeilendeForsøk = it.antallRetriesForsøkt(),
                                    feilmelding = hentFeilmeldingHvisBehov(
                                        it.status(),
                                        it.jobbId(),
                                        flytJobbRepository
                                    ),
                                    beskrivelse = it.beskrivelse(),
                                    navn = it.navn()
                                )
                            })
                    // Henter denne ut etter status er utledet for å være sikker på at dataene er i rett tilstand
                    behandling = behandlingRepository.hent(req)
                    val flyt = utledType(behandling.typeBehandling).flyt()

                    val stegGrupper: Map<StegGruppe, List<StegType>> =
                        flyt.stegene().groupBy { steg -> steg.gruppe }
                    val aktivtSteg = behandling.aktivtSteg()
                    var erFullført = true
                    val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

                    val alleAvklaringsbehovInkludertFrivillige = FrivilligeAvklaringsbehov(
                        avklaringsbehovene,
                        flyt, aktivtSteg
                    )
                    val alleAvklaringsbehov = alleAvklaringsbehovInkludertFrivillige.alle()

                    BehandlingFlytOgTilstandDto(
                        flyt = stegGrupper.map { (gruppe, steg) ->
                            erFullført = erFullført && gruppe != aktivtSteg.gruppe
                            FlytGruppe(
                                stegGruppe = gruppe,
                                skalVises = gruppeVisningService.skalVises(gruppe, behandling.id),
                                erFullført = erFullført,
                                steg = steg.map { stegType ->
                                    FlytSteg(
                                        stegType = stegType,
                                        avklaringsbehov = alleAvklaringsbehov
                                            .filter { avklaringsbehov -> avklaringsbehov.skalLøsesISteg(stegType) }
                                            .map { behov ->
                                                AvklaringsbehovDTO(
                                                    behov.definisjon,
                                                    behov.status(),
                                                    emptyList()
                                                )
                                            },
                                    )
                                }
                            )
                        },
                        aktivtSteg = aktivtSteg,
                        aktivGruppe = aktivtSteg.gruppe,
                        behandlingVersjon = behandling.versjon,
                        prosessering = prosessering,
                        visning = utledVisning(
                            alleAvklaringsbehovInkludertFrivillige = alleAvklaringsbehovInkludertFrivillige,
                            status = prosessering.status,
                            behandlingStatus = behandling.status()
                        ),
                        nesteBehandlingId = dokumentHåndteringBehnandlingId
                    )
                }
                respond(dto)
            }
        }
        route("/{referanse}/resultat") {
            authorizedGet<BehandlingsreferansePathParam, BehandlingResultatDto>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "referanse",
                        journalpostIdFraBehandlingResolver(repositoryRegistry, dataSource)
                    )
                )
            ) { _ ->
                val dto = dataSource.transaction(readOnly = true) { _ ->

                    BehandlingResultatDto()
                }
                respond(dto)
            }
        }

        route("/{referanse}/sett-på-vent") {
            authorizedPost<BehandlingsreferansePathParam, BehandlingResultatDto, SettPåVentRequest>(
                AuthorizationParamPathConfig(
                    Operasjon.SAKSBEHANDLE,
                    journalpostPathParam = JournalpostPathParam(
                        "referanse",
                        resolver = journalpostIdFraBehandlingResolver(repositoryRegistry, dataSource),
                    ),
                    avklaringsbehovKode = Definisjon.MANUELT_SATT_PÅ_VENT.kode.name
                )
            ) { request, body ->
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val taSkriveLåsRepository = repositoryProvider.provide(TaSkriveLåsRepository::class)
                    val lås = taSkriveLåsRepository.lås(request)
                    BehandlingTilstandValidator(
                        BehandlingReferanseService(repositoryProvider.provide(BehandlingRepository::class)),
                        FlytJobbRepository(connection)
                    ).validerTilstand(
                        request,
                        body.behandlingVersjon
                    )


                    MDC.putCloseable("behandlingId", lås.id.toString()).use {
                        BehandlingHendelseHåndterer(repositoryRegistry.provider(connection), gatewayProvider).håndtere(
                            key = lås.id,
                            hendelse = BehandlingSattPåVent(
                                frist = body.frist,
                                begrunnelse = body.begrunnelse,
                                behandlingVersjon = body.behandlingVersjon,
                                grunn = body.grunn,
                                bruker = bruker()
                            )
                        )
                        taSkriveLåsRepository.verifiserSkrivelås(lås)
                    }
                }
                respondWithStatus(HttpStatusCode.NoContent)
            }
        }
        route("/{referanse}/vente-informasjon") {
            authorizedGet<BehandlingsreferansePathParam, Venteinformasjon>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "referanse",
                        journalpostIdFraBehandlingResolver(repositoryRegistry, dataSource)
                    )
                )
            ) { request ->

                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
                    val avklaringsbehovRepository = repositoryProvider.provide(AvklaringsbehovRepository::class)

                    val behandling = behandlingRepository.hent(request)
                    val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

                    val ventepunkter = avklaringsbehovene.hentVentepunkter()
                    if (avklaringsbehovene.erSattPåVent()) {
                        val avklaringsbehov = ventepunkter.first()
                        Venteinformasjon(
                            avklaringsbehov.frist(),
                            avklaringsbehov.begrunnelse(),
                            avklaringsbehov.grunn()
                        )
                    } else {
                        null
                    }
                }
                if (dto == null) {
                    respondWithStatus(HttpStatusCode.NoContent)
                } else {
                    respond(dto)
                }
            }
        }
    }
}

private fun hentFeilmeldingHvisBehov(
    status: JobbStatus,
    jobbId: Long,
    flytJobbRepository: FlytJobbRepository
): String? {
    if (status == JobbStatus.FEILET) {
        return flytJobbRepository.hentFeilmeldingForOppgave(jobbId)
    }
    return null
}

private fun utledStatus(oppgaver: List<JobbInput>): ProsesseringStatus {
    if (oppgaver.isEmpty()) {
        return ProsesseringStatus.FERDIG
    }
    if (oppgaver.any { it.status() == JobbStatus.FEILET }) {
        return ProsesseringStatus.FEILET
    }
    return ProsesseringStatus.JOBBER
}

private fun utledVisning(
    alleAvklaringsbehovInkludertFrivillige: FrivilligeAvklaringsbehov,
    status: ProsesseringStatus,
    behandlingStatus: Status,
): Visning {
    val jobber = status in listOf(ProsesseringStatus.JOBBER, ProsesseringStatus.FEILET)
    val påVent = alleAvklaringsbehovInkludertFrivillige.erSattPåVent()

    return if (jobber) {
        Visning(
            visVentekort = påVent,
            readOnly = true,
        )
    } else {
        Visning(
            visVentekort = påVent,
            readOnly = behandlingStatus.erAvsluttet(),
        )
    }
}
