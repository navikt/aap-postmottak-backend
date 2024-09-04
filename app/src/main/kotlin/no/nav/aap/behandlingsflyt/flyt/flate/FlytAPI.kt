package no.nav.aap.behandlingsflyt.flyt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.flate.visning.DynamiskStegGruppeVisningService
import no.nav.aap.behandlingsflyt.flyt.flate.visning.Prosessering
import no.nav.aap.behandlingsflyt.flyt.flate.visning.ProsesseringStatus
import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbStatus
import no.nav.aap.verdityper.flyt.StegGruppe
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

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
                                    avklaringsbehov = alleAvklaringsbehovInkludertFrivillige.alle()
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
                    behandlingVersjon = 1
                )}
                respond(dto)
            }
        }
        route("/{referanse}/resultat") {
            get<BehandlingReferanse, BehandlingResultatDto> { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->

                    BehandlingResultatDto()
                }
                respond(dto)
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
