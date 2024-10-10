package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet

fun NormalOpenAPIRoute.kategoriseringApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/kategorisering") {
        authorizedGet<JournalpostId, KategoriseringGrunnlagDto>(JournalpostPathParam("referanse")) { req ->
            val vurdering = dataSource.transaction(readOnly = true) {
                BehandlingRepositoryImpl(it).hent(req)
            }
            respond(
                KategoriseringGrunnlagDto(
                    vurdering.vurderinger.kategorivurdering
                        ?.avklaring?.let(::KategoriseringVurderingDto),
                    listOf(1, 2)
                )
            )
        }
    }

}
