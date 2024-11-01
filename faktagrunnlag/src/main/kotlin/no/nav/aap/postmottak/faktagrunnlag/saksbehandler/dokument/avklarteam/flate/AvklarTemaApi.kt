package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam.AvklarTemaRepository
import no.nav.aap.postmottak.journalPostResolverFactory
import no.nav.aap.postmottak.klient.gosysoppgave.Oppgaveklient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.tilgang.authorizedGet
import java.net.URI
import javax.sql.DataSource


fun NormalOpenAPIRoute.avklarTemaApi(dataSource: DataSource) {
    route("/api/behandling/{referanse}") {
        route("/grunnlag/avklarTemaVurdering") {
            authorizedGet<BehandlingsreferansePathParam, AvklarTemaGrunnlagDto>(
                journalPostResolverFactory(dataSource)
            ) { req ->
                val grunnlag = dataSource.transaction(readOnly = true) {
                    val behandling = BehandlingRepositoryImpl(it).hent(req)
                    val journalpost =
                        JournalpostRepositoryImpl(it).hentHvisEksisterer(behandling.id)
                    require(journalpost != null) { "Fant ikke journalpost" }
                    val arkivDokumenter = journalpost.finnArkivVarianter()
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
                val aktivIdent = dataSource.transaction(readOnly = true) { connection ->
                    JournalpostRepositoryImpl(connection).hentHvisEksisterer(req)?.person?.aktivIdent()
                }
                require(aktivIdent != null) { "Fant ikke personident for journalpost" }
                Oppgaveklient().opprettOppgave(req, aktivIdent.identifikator)

                val url = URI.create(requiredConfigForKey("gosys.url"))
                respond(EndreTemaResponse(url.toString()))

            }
        }
    }
}


data class EndreTemaResponse(
    val redirectUrl: String
)
