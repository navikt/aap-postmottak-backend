package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.grovvurdering.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.komponenter.dbconnect.transaction
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*

fun getPdfInputStream(): InputStream {
    val base64Pdf =
        "JVBERi0xLjAKMSAwIG9iajw8L1BhZ2VzIDIgMCBSPj5lbmRvYmogMiAwIG9iajw8L0tpZHNbMyAwIFJdL0NvdW50IDE+PmVuZG9iaiAzIDAgb2JqPDwvTWVkaWFCb3hbMCAwIDMgM10+PmVuZG9iagp0cmFpbGVyPDwvUm9vdCAxIDAgUj4+Cg=="
    val decode = Base64.getDecoder().decode(base64Pdf)
    return ByteArrayInputStream(decode)
}

fun NormalOpenAPIRoute.grovvurderingApi(dataSource: HikariDataSource) {
    route("/grovvurdering") {
        get<JournalpostId, GrovvurderingDto> { req ->
            val vurdering = dataSource.transaction {
                BehandlingRepositoryImpl(it).hent(req)
            }
            respond(
                GrovvurderingDto(
                    vurdering.vurderinger.grovkategorivurdering?.vurdering,
                    listOf(
                        getPdfInputStream(),
                        getPdfInputStream()
                    )
                )
            )
        }
    }

}
