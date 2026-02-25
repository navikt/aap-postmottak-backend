package no.nav.aap.postmottak.api.flyt

import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.api.flyt.service.RedigitaliseringService
import no.nav.aap.postmottak.api.journalpostIdFraBehandlingResolver
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.tilgang.plugin.kontrakt.Saksreferanse
import javax.sql.DataSource

fun NormalOpenAPIRoute.redigitaliseringAPI(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("/api/redigitalisering") {
        authorizedPost<Unit, Unit, RedigitaliserRequest>(
            AuthorizationBodyPathConfig(
                Operasjon.SE, // TODO: Skriveoperasjon krever behandlingsreferanse - bruker 'SE' enn så lenge
                journalpostIdResolver = journalpostIdFraBehandlingResolver(repositoryRegistry, dataSource),
            )
        ) { _, req ->
            dataSource.transaction { connection ->
                val service = RedigitaliseringService.konstruer(repositoryRegistry.provider(connection))
                service.redigitaliser(req.journalpostId, req.saksnummer)
            }
            respondWithStatus(HttpStatusCode.Accepted)
        }
    }
}

class RedigitaliserRequest(
    @param:JsonProperty(value = "journalpostId", required = true) val journalpostId: Long, val saksnummer: String
) : Saksreferanse {
    override fun hentSaksreferanse(): String {
        return saksnummer
    }
}