package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.SafRestClient
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.DokumentResponsDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.HentDokumentDTO
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.verdityper.dokument.DokumentInfoId


fun NormalOpenAPIRoute.dokumentApi() {
    route("/api/dokumenter") {
        route("/{journalpostId}/{dokumentinfoId") {
            get<HentDokumentDTO, DokumentResponsDTO> { req ->
                val journalpostId = req.journalpostId
                val dokumentInfoId = req.dokumentinfoId

                val token = token()
                val gateway = SafRestClient.withDefaultRestClient()
                val dokumentRespons =
                    gateway.hentDokument(
                        JournalpostId(journalpostId),
                        DokumentInfoId(dokumentInfoId),
                        currentToken = token
                    )
                pipeline.context.response.headers.append(
                    name = "Content-Disposition", value = "inline; filename=${dokumentRespons.filnavn}"
                )

                respond(DokumentResponsDTO(stream = dokumentRespons.dokument))
            }
        }
    }
}
