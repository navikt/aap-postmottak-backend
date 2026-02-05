package no.nav.aap.postmottak.api.flyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.gateway.DokumentOboGateway
import no.nav.aap.postmottak.gateway.SafVariantformat
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.flate.DokumentResponsDTO
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.flate.HentDokumentDTO
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.LoggerFactory
import javax.sql.DataSource

fun NormalOpenAPIRoute.redigitaliseringAPI(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    val log = LoggerFactory.getLogger("RedigitaliseringAPI")

    route("/api/redigitalisering") {
        route("/{journalpostId}") {
            authorizedPost<>()<HentDokumentDTO, DokumentResponsDTO>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "journalpostId"
                    )
                )
            ) { req ->
                dataSource.transaction() { connection ->

                    val journalpostId = req.journalpostId
                    val dokumentInfoId = req.dokumentinfoId
                    val flytJobbRepository =

                    val token = token()

                    val behandling = behandlingRepository.hent(kontekst.behandlingId)

                    val dokumentbehandlingId =
                        behandlingRepository.opprettBehandling(behandling.journalpostId, TypeBehandling.DokumentHåndtering)
                    kopierer.overfør(kontekst.behandlingId, dokumentbehandlingId)

                    log.info("Legger til jobb for redigitalisering")

                    flytJobbRepository.leggTil(
                        JobbInput(ProsesserBehandlingJobbUtfører)
                            .forBehandling(behandling.journalpostId.referanse, dokumentbehandlingId.id).medCallId()
                    )

                }
                respond(DokumentResponsDTO(stream = dokumentRespons.dokument))
            }
        }
    }
}