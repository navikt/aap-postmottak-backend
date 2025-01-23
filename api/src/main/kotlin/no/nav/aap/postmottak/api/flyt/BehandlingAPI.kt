package no.nav.aap.postmottak.api.flyt

import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.postmottak.faktagrunnlag.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.flyt.utledType
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.behandlingApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}") {
            authorizedGet<BehandlingsreferansePathParam, DetaljertBehandlingDTO>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "referanse",
                        journalpostIdFraBehandlingResolver(dataSource)
                    )
                )
            ) { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
                    val avklaringsbehovRepository = repositoryProvider.provide(AvklaringsbehovRepository::class)

                    val behandling = behandling(behandlingRepository, req)
                    val flyt = utledType(behandling.typeBehandling).flyt()
                    DetaljertBehandlingDTO(
                        referanse = req,
                        type = behandling.typeBehandling.name,
                        status = behandling.status(),
                        opprettet = behandling.opprettetTidspunkt,
                        skalForberede = behandling.harIkkeVærtAktivitetIDetSiste(),

                        avklaringsbehov = FrivilligeAvklaringsbehov(
                            avklaringsbehov(
                                avklaringsbehovRepository,
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
            authorizedGet<BehandlingsreferansePathParam, DetaljertBehandlingDTO>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "referanse",
                        journalpostIdFraBehandlingResolver(dataSource)
                    )
                )
            ) { req ->
                dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
                    
                    val behandling = behandling(behandlingRepository, req)
                    val flytJobbRepository = FlytJobbRepository(connection)
                    if (flytJobbRepository.hentJobberForBehandling(behandling.id.toLong()).isEmpty()) {
                        flytJobbRepository.leggTil(
                            JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(
                                behandling.journalpostId.referanse,
                                behandling.id.id
                            )
                        )
                    }
                }
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
        // TODO: Kun for test
        @Suppress("UnauthorizedPost")
        post<Unit, BehandlingsreferansePathParam, JournalpostDto> { _, body ->
            val referanse = dataSource.transaction { connection ->
                val repositoryProvider = RepositoryProvider(connection)
                val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
                
                val behandlingId =
                    behandlingRepository.opprettBehandling(JournalpostId(body.referanse), TypeBehandling.Journalføring)
                FlytJobbRepository(connection).leggTil(
                    JobbInput(ProsesserBehandlingJobbUtfører)
                        .forBehandling(body.referanse, behandlingId.id).medCallId()
                )
                behandlingRepository.hent(behandlingId).referanse
            }
            respond(referanse)
        }
    }
}

private fun behandling(behandlingRepository: BehandlingRepository, req: BehandlingsreferansePathParam): Behandling {
    return behandlingRepository.hent(req)
}

private fun avklaringsbehov(avklaringsbehovRepository: AvklaringsbehovRepository, behandlingId: BehandlingId): Avklaringsbehovene {
    return avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
}

class JournalpostDto(
    @JsonProperty(
        "referanse", required = true,
        defaultValue = "0"
    ) val referanse: Long
)