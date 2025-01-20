package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.AvklarTemaRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.LoggerFactory
import tilgang.Operasjon
import java.net.URI
import javax.sql.DataSource

val log = LoggerFactory.getLogger("AvklarTemaApi")

fun NormalOpenAPIRoute.avklarTemaApi(dataSource: DataSource) {
    route("/api/behandling/{referanse}") {
        route("/grunnlag/avklarTemaVurdering") {
            authorizedGet<BehandlingsreferansePathParam, AvklarTemaGrunnlagDto>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "referanse",
                        journalpostIdFraBehandlingResolver(dataSource)
                    )
                )
            ) { req ->
                val grunnlag = dataSource.transaction(readOnly = true) {
                    val repositoryProvider = RepositoryProvider(it)
                    val behandling = repositoryProvider.provide(BehandlingRepository::class).hent(req)
                    val journalpost =
                        repositoryProvider.provide(JournalpostRepository::class).hentHvisEksisterer(behandling.id)
                    require(journalpost != null) { "Fant ikke journalpost" }
                    val arkivDokumenter = journalpost.finnArkivVarianter()
                    AvklarTemaGrunnlagDto(
                        repositoryProvider.provide(AvklarTemaRepository::class)
                            .hentTemaAvklaring(behandling.id)?.skalTilAap?.let(::AvklarTemaVurderingDto),
                        arkivDokumenter.map { it.dokumentInfoId.dokumentInfoId }
                    )
                }
                respond(grunnlag)
            }
        }
        // TODO: Denne bør kanskje gjøres frontend?
        route("/endre-tema") {
            authorizedPost<BehandlingsreferansePathParam, EndreTemaResponse, Unit>(
                AuthorizationParamPathConfig(
                    Operasjon.SAKSBEHANDLE,
                    journalpostPathParam = JournalpostPathParam(
                        "referanse",
                        resolver = journalpostIdFraBehandlingResolver(dataSource)
                    ),
                    avklaringsbehovKode = Definisjon.ENDRE_TEMA.kode.name
                )
            ) { req, _ ->
                val url = URI.create(requiredConfigForKey("gosys.url"))
                respond(EndreTemaResponse(url.toString()))
            }
        }
    }
}


data class EndreTemaResponse(
    val redirectUrl: String
)
