package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.flate


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

fun NormalOpenAPIRoute.struktureringApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/strukturering") {
        authorizedGet<JournalpostId, StruktureringGrunnlagDto>(JournalpostPathParam("referanse")) { req ->
            val behandling = dataSource.transaction(readOnly = true) {
                BehandlingRepositoryImpl(it).hent(req)
            }

            check(behandling.harBlittKategorisert()) { "Behandlingen mangler kategorisering" }

            respond(
                StruktureringGrunnlagDto(
                    behandling.vurderinger.struktureringsvurdering
                        ?.vurdering?.let(::StruktureringVurderingDto),
                    behandling.vurderinger.kategorivurdering!!.avklaring,
                    listOf(1, 2)
                )
            )
        }
    }

}
