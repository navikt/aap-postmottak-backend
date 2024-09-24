package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.komponenter.dbconnect.transaction

fun NormalOpenAPIRoute.struktureringApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/strukturering") {
        get<JournalpostId, StruktureringGrunnlagDto> { req ->
            val behandling = dataSource.transaction {
                BehandlingRepositoryImpl(it).hent(req)
            }

            check(behandling.harBlittKategorisert()) { "Behandlingen mangler kategorisering" }

            respond(
                StruktureringGrunnlagDto(
                    behandling.vurderinger.struktureringsvurdering
                        ?.vurdering?.let(::StruktureringVurderingDto),
                    behandling.vurderinger.kategorivurdering!!.vurdering,
                    listOf(1,2)
                )
            )
        }
    }

}
