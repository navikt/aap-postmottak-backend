package no.nav.aap.postmottak.api.auditlog

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedPost
import javax.sql.DataSource

fun NormalOpenAPIRoute.auditlogApi(dataSource: DataSource) {
    route("/api/journalpost") {
        route("/{referanse}/auditlog") {
            authorizedPost<JournalpostId, Unit, Unit>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "referanse"
                    )
                ),
                DefaultAuditLogConfig.fraJournalpostPathParam("referanse", dataSource)
            ) { _, _ ->
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
    }
}