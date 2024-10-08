package no.nav.aap.postmottak.flyt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingHendelseHåndterer
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.bruker
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbStatus
import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.postmottak.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.behandling.avklaringsbehov.BehandlingTilstandValidator
import no.nav.aap.postmottak.behandling.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.postmottak.flyt.flate.visning.DynamiskStegGruppeVisningService
import no.nav.aap.postmottak.flyt.flate.visning.Prosessering
import no.nav.aap.postmottak.flyt.flate.visning.ProsesseringStatus
import no.nav.aap.postmottak.flyt.utledType
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.slf4j.MDC

fun NormalOpenAPIRoute.flytApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/flyt") {
            get<JournalpostId, BehandlingFlytOgTilstandDto> { req ->
                val dto = dataSource.transaction { connection ->
                    val behandling = BehandlingRepositoryImpl(connection).hent(req)
                    val flytJobbRepository = FlytJobbRepository(connection)
                    val gruppeVisningService = DynamiskStegGruppeVisningService(connection)
                    val flyt = utledType(behandling.typeBehandling).flyt()

                    var erFullført = true

                    val stegGrupper: Map<StegGruppe, List<StegType>> =
                        flyt.stegene().groupBy { steg -> steg.gruppe }

                    val aktivtSteg = behandling.aktivtSteg()

                    val avklaringsbehovene = avklaringsbehov(
                        connection,
                        behandling.id
                    )

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
                        prosessering = Prosessering(
                            status = ProsesseringStatus.FERDIG,
                            ventendeOppgaver = emptyList()
                        ),
                        behandlingVersjon = behandling.versjon
                    )
                }
                respond(dto)
            }
        }
        route("/{referanse}/resultat") {
            get<JournalpostId, BehandlingResultatDto> { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->

                    BehandlingResultatDto()
                }
                respond(dto)
            }
        }

        route("/{referanse}/sett-på-vent") {
            post<JournalpostId, BehandlingResultatDto, SettPåVentRequest> { request, body ->
                dataSource.transaction { connection ->
                    val taSkriveLåsRepository = TaSkriveLåsRepository(connection)
                    val lås = taSkriveLåsRepository.lås(request.referanse)
                    BehandlingTilstandValidator(connection).validerTilstand(
                        request,
                        body.behandlingVersjon
                    )


                    MDC.putCloseable("behandlingId", lås.behandlingSkrivelås.id.toString()).use {
                        BehandlingHendelseHåndterer(connection).håndtere(
                            key = lås.behandlingSkrivelås.id,
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
            get<JournalpostId, Venteinformasjon> { request ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val behandling = BehandlingRepositoryImpl(connection).hent(request)

                    val avklaringsbehovene = avklaringsbehov(connection, behandling.id)

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


private fun avklaringsbehov(connection: DBConnection, behandlingId: BehandlingId): Avklaringsbehovene {
    return AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandlingId)
}
