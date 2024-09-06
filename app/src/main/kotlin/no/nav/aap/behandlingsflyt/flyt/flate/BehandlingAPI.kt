package no.nav.aap.behandlingsflyt.flyt.flate

import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.behandlingsflyt.server.respondWithStatus
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import javax.sql.DataSource

fun NormalOpenAPIRoute.behandlingApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}") {
            get<JournalpostId, DetaljertBehandlingDTO> { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val behandling = behandling(connection, req)
                    val flyt = utledType(behandling.typeBehandling).flyt()
                    DetaljertBehandlingDTO(
                        referanse = behandling.referanse,
                        type = behandling.typeBehandling.name,
                        status = behandling.status(),
                        opprettet = behandling.opprettetTidspunkt,

                        avklaringsbehov = FrivilligeAvklaringsbehov(
                            avklaringsbehov(
                                connection,
                                behandling.id
                            ),
                            flyt,
                            behandling.aktivtSteg()
                        ).alle().map { avklaringsbehov ->
                            AvklaringsbehovDTO(
                                definisjon = avklaringsbehov.definisjon,
                                status = avklaringsbehov.status(),
                                endringer = avklaringsbehov.historikk.map { endring ->
                                    EndringDTO(
                                        status = endring.status,
                                        tidsstempel = endring.tidsstempel,
                                        begrunnelse = endring.begrunnelse + "Noe",
                                        endretAv = endring.endretAv
                                    )
                                }
                            )
                        },
                        aktivtSteg = behandling.stegHistorikk().last().steg(),
                        versjon = behandling.versjon
                    )
                }
                respond(dto)
            }
        }
        route("/{referanse}/forbered") {
            get<JournalpostId, DetaljertBehandlingDTO> { req ->
                dataSource.transaction { connection ->
                    val behandling = behandling(connection, req)
                    val flytJobbRepository = FlytJobbRepository(connection)
                    if (flytJobbRepository.hentJobberForBehandling(behandling.id).isEmpty()) {
                        flytJobbRepository.leggTil(
                            JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(
                                behandling.id
                            )
                        )
                    }
                }
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
        // TODO: Kun for test
        post<Unit, JournalpostDto, JournalpostDto> { _, body ->
            dataSource.transaction { connection ->
                val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(JournalpostId((body.referanse)))
                FlytJobbRepository(connection).leggTil(
                    JobbInput(ProsesserBehandlingJobbUtfører)
                        .forBehandling(behandling.id).medCallId()
                )

            }
            respond(JournalpostDto(body.referanse))
        }
    }
}

private fun behandling(connection: DBConnection, req: JournalpostId): Behandling {
    return BehandlingRepositoryImpl(connection).hent(req)
}

private fun avklaringsbehov(connection: DBConnection, behandlingId: BehandlingId): Avklaringsbehovene {
    return AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandlingId)
}

class JournalpostDto(
    @JsonProperty(
        "referanse", required = true,
        defaultValue = "0"
    ) val referanse: Long
)