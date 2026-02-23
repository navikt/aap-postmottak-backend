package no.nav.aap.postmottak.api.flyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.api.flyt.service.RedigitaliseringService
import no.nav.aap.postmottak.api.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import javax.sql.DataSource

fun NormalOpenAPIRoute.redigitaliseringAPI(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("/api/redigitalisering") {
        route("/{referanse}") {
            authorizedPost<BehandlingsreferansePathParam, Unit, JournalpostId>(
                AuthorizationParamPathConfig(
                    operasjon = Operasjon.SAKSBEHANDLE,
                    journalpostPathParam = JournalpostPathParam(
                        "referanse", journalpostIdFraBehandlingResolver(repositoryRegistry, dataSource)
                    ),
                    avklaringsbehovKode = Definisjon.DIGITALISER_DOKUMENT.kode.name
                )
            ) { _, body ->
                dataSource.transaction { connection ->
                    val service = RedigitaliseringService.konstruer(repositoryRegistry.provider(connection))
                    service.redigitaliser(body.referanse)
                }
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
    }
}