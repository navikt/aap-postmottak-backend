package no.nav.aap.postmottak.api.test

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.DokarkivGateway
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import javax.sql.DataSource

data class DokarkivJournalpostParams(
    @param:PathParam("Kildejournalpost-ID")
    val journalpostId: Long
)

data class KnyttTilAnnenSakTestRequest(
    val fagsakId: String,
    val journalfoerendeEnhet: String
)

data class KnyttTilAnnenSakTestResponse(val nyJournalpostId: JournalpostId)

fun NormalOpenAPIRoute.dokarkivTestApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    if (Miljø.er() == MiljøKode.PROD) return

    route("/test/dokarkiv/journalposter/{journalpostId}/knytt-til-annen-sak") {
        @Suppress("UnauthorizedPost") // Kun tilgjengelig utenfor prod, bak Entra-innlogging.
        post<DokarkivJournalpostParams, KnyttTilAnnenSakTestResponse, KnyttTilAnnenSakTestRequest> { params, request ->
            if (request.fagsakId.isBlank() || request.journalfoerendeEnhet.isBlank()) {
                throw UgyldigForespørselException(
                    message = "Saksnummer og journalførende enhet må være oppgitt."
                )
            }
            val journalpost = dataSource.transaction { connection ->
                JournalpostService.konstruer(repositoryRegistry.provider(connection), gatewayProvider)
                    .hentJournalpost(JournalpostId(params.journalpostId))
            }
            val nyJournalpostId = gatewayProvider.provide<DokarkivGateway>()
                .knyttTilAnnenSak(journalpost, request.fagsakId, request.journalfoerendeEnhet)
            respond(KnyttTilAnnenSakTestResponse(nyJournalpostId))
        }
    }
}
