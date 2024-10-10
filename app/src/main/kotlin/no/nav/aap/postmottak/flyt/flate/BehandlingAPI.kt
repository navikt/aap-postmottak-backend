package no.nav.aap.postmottak.flyt.flate

import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.postmottak.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.behandling.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.postmottak.flyt.utledType
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandling
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import javax.sql.DataSource

fun NormalOpenAPIRoute.behandlingApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}") {
            authorizedGet<JournalpostId, DetaljertBehandlingDTO>(JournalpostPathParam("referanse")) { req ->
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
            authorizedGet<JournalpostId, DetaljertBehandlingDTO>(JournalpostPathParam("referanse")) { req ->
                dataSource.transaction { connection ->
                    val behandling = behandling(connection, req)
                    val flytJobbRepository = FlytJobbRepository(connection)
                    if (flytJobbRepository.hentJobberForBehandling(behandling.id.toLong()).isEmpty()) {
                        flytJobbRepository.leggTil(
                            JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(
                                null,
                                behandling.id.toLong()
                            )
                        )
                    }
                }
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
        // TODO: Kun for test
        @Suppress("UnauthorizedPost")
        post<Unit, JournalpostDto, JournalpostDto> { _, body ->
            dataSource.transaction { connection ->
                val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(JournalpostId((body.referanse)))
                FlytJobbRepository(connection).leggTil(
                    JobbInput(ProsesserBehandlingJobbUtfører)
                        .forBehandling(null, behandling.id.toLong()).medCallId()
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