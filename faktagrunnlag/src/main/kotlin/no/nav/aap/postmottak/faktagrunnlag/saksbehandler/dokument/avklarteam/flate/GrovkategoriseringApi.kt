package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam.AvklarTemaRepository
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.saf.graphql.SafGraphqlClient
import no.nav.aap.postmottak.saf.graphql.tilJournalpost
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet

fun NormalOpenAPIRoute.avklarTemaVurderingApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/avklarTemaVurdering") {
        authorizedGet<JournalpostId, AvklarTemaGrunnlagDto>(JournalpostPathParam("referanse")) { req ->
            val token = token()
            val grunnlag = dataSource.transaction(readOnly = true) {
                val behandling = BehandlingRepositoryImpl(it).hent(req)
                val journalpost = SafGraphqlClient.withOboRestClient().hentJournalpost(behandling.journalpostId, token)
                val arkivDokumenter = journalpost.tilJournalpost().finnArkivVarianter()
                AvklarTemaGrunnlagDto(
                    AvklarTemaRepository(it).hentTemaAvklaring(behandling.id)?.skalTilAap?.let(::AvklarTemaVurderingDto),
                    arkivDokumenter.map { it.dokumentInfoId.dokumentInfoId }
                )
            }
            respond(grunnlag)
        }
    }
}
