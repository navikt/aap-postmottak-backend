package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.bruker
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam.AvklarTemaRepository
import no.nav.aap.postmottak.klient.gosysoppgave.Oppgaveklient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.saf.graphql.SafGraphqlClient
import no.nav.aap.postmottak.saf.graphql.tilJournalpost
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import java.net.URI

fun NormalOpenAPIRoute.avklarTemaApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}") {
        route("/grunnlag/avklarTemaVurdering") {
            authorizedGet<JournalpostId, AvklarTemaGrunnlagDto>(JournalpostPathParam("referanse")) { req ->
                val token = token()
                val grunnlag = dataSource.transaction(readOnly = true) {
                    val behandling = BehandlingRepositoryImpl(it).hent(req)
                    val journalpost =
                        SafGraphqlClient.withOboRestClient().hentJournalpost(behandling.journalpostId, token)
                    val arkivDokumenter = journalpost.tilJournalpost().finnArkivVarianter()
                    AvklarTemaGrunnlagDto(
                        AvklarTemaRepository(it).hentTemaAvklaring(behandling.id)?.skalTilAap?.let(::AvklarTemaVurderingDto),
                        arkivDokumenter.map { it.dokumentInfoId.dokumentInfoId }
                    )
                }
                respond(grunnlag)
            }
        }
        route("/endre-tema") {
            @Suppress("UnauthorizedPost")
            post<JournalpostId, EndreTemaResponse, Unit> { req, _ ->
                val ident = bruker().ident
                Oppgaveklient().opprettOppgave(req, ident)
                val url = URI.create(requiredConfigForKey("gosys.url"))
                respond(EndreTemaResponse(url.toString()))
            }
        }
    }
}


data class EndreTemaResponse(
    val redirectUrl: String
)
