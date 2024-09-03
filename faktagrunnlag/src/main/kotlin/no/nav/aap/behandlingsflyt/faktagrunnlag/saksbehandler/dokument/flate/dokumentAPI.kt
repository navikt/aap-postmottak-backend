package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.flate


import com.papsign.ktor.openapigen.content.type.binary.BinaryResponse
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.SafRestClient
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.DokumentResponsDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.HentDokumentDTO
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.verdityper.dokument.DokumentInfoId
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*

@BinaryResponse(["application/pdf"])
data class PdfResponse(val pdfInputStream: InputStream)

fun NormalOpenAPIRoute.dokumentApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}") {
            get<BehandlingReferanse, DokumentBehandlingDto> { req ->
                // TODO hent faktisk behandling
                respond(
                    DokumentBehandlingDto(
                        "1",
                        "1",
                        "1",
                        1,
                    )
                )
            }
        }
        route("/{referanse}/dokument") {
            get<BehandlingReferanse, PdfResponse> { req ->
                // TODO hent journalpostid fra dokument behandling
                // TODO proxy dokument fra joark
                val base64Pdf =
                    "JVBERi0xLjAKMSAwIG9iajw8L1BhZ2VzIDIgMCBSPj5lbmRvYmogMiAwIG9iajw8L0tpZHNbMyAwIFJdL0NvdW50IDE+PmVuZG9iaiAzIDAgb2JqPDwvTWVkaWFCb3hbMCAwIDMgM10+PmVuZG9iagp0cmFpbGVyPDwvUm9vdCAxIDAgUj4+Cg=="
                val decode = Base64.getDecoder().decode(base64Pdf)

                respond(PdfResponse(ByteArrayInputStream(decode)))
            }
        }
    }
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
