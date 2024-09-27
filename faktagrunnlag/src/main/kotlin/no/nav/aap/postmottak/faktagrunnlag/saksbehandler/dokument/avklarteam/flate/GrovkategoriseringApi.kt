package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.postmottak.saf.graphql.SafGraphqlClient
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.token

fun NormalOpenAPIRoute.avklarTemaVurderingApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/avklarTemaVurdering") {
        get<JournalpostId, AvklarTemaGrunnlagDto> { req ->
            val token = token()
            val grunnlag = dataSource.transaction {
                val behandling = BehandlingRepositoryImpl(it).hent(req)
                val journalpost = SafGraphqlClient.withOboRestClient().hentJournalpost(behandling.journalpostId, token)
                val arkivDokumenter = journalpost.finnArkivVarianter()
                AvklarTemaGrunnlagDto(
                    behandling.vurderinger.avklarTemaVurdering
                        ?.avklaring?.let(::AvklarTemaVurderingDto),
                    arkivDokumenter.map { it.dokumentInfoId.dokumentInfoId }
                )
            }
            respond(grunnlag)
        }
    }
}
