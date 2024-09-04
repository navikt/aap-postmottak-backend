package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.flate


import com.papsign.ktor.openapigen.content.type.binary.BinaryResponse
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.grovvurdering.flate.kategoriseringApi
import java.io.InputStream

@BinaryResponse(["application/pdf"])
data class PdfResponse(val pdfInputStream: InputStream)

fun NormalOpenAPIRoute.dokumentApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}") {
        }
        kategoriseringApi(dataSource)
    }
}

