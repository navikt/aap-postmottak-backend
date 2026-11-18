package no.nav.aap.postmottak.api.test

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.postmottak.gateway.JournalpostOboGateway
import no.nav.aap.postmottak.gateway.SafTilknyttetJournalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId

fun NormalOpenAPIRoute.safTestApi(gatewayProvider: GatewayProvider) {
    if (Miljø.er() != MiljøKode.DEV) return

    route("/test/saf/tilknyttede-journalposter/{dokumentInfoId}") {
        @Suppress("UnauthorizedGet") // SAF kontrollerer tilgangen med OBO-tokenet.
        get<TilknyttedeJournalposterRequest, List<SafTilknyttetJournalpost>> { req ->
            val journalposter = gatewayProvider.provide<JournalpostOboGateway>()
                .hentTilknyttedeJournalposter(DokumentInfoId(req.dokumentInfoId), token())
            respond(journalposter)
        }
    }
}
