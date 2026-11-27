package no.nav.aap.postmottak.api.faktagrunnlag.dokument

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.postmottak.api.auditlog.DefaultAuditLogConfig
import no.nav.aap.postmottak.api.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.gateway.JournalpostOboGateway
import no.nav.aap.postmottak.gateway.SafDokumentoversikt
import no.nav.aap.postmottak.gateway.SafVariantformat
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.JournalpostTilgangRequest
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.TilgangGateway
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

data class JournalposterParams(
    @param:PathParam("Behandlingsreferanse. Listen omfatter bare personens AAP-journalposter.") val referanse: UUID,
    @param:QueryParam("Antall journalposter, fra 1 til 100. Standard er 20.") val foerste: Int? = null,
    @param:QueryParam("Sluttpeker fra forrige side i SAF.") val etter: String? = null
)

/**
 * AAP-journalposter i alle journalstatuser, sortert nyeste først etter opprettelsesdato i SAF.
 * Journalposter uten lesetilgang og skjermede metadata utelates. En side kan derfor være tom selv om neste side finnes.
 * Bruk sluttpeker fra svaret som etter i neste kall.
 */
data class JournalposterRespons(
    val journalposter: List<JournalpostOversiktDto>,
    val sluttpeker: String?,
    val finnesNesteSide: Boolean
)

data class JournalpostOversiktDto(
    val journalpostId: String,
    val tittel: String?,
    val dato: LocalDateTime,
    val tema: String,
    val dokumenter: List<JournalpostDokumentDto>
)

data class JournalpostDokumentDto(
    val dokumentInfoId: String,
    val tittel: String?,
    val kanForhaandsvises: Boolean
)

fun NormalOpenAPIRoute.journalposterApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    route("/api/behandling/{referanse}/journalposter") {
        authorizedGet<JournalposterParams, JournalposterRespons>(
            AuthorizationParamPathConfig(
                journalpostPathParam = JournalpostPathParam(
                    "referanse", journalpostIdFraBehandlingResolver(repositoryRegistry, dataSource)
                )
            ),
            DefaultAuditLogConfig.fraBehandlingPathParam("referanse", dataSource, repositoryRegistry)
        ) { req ->
            val foerste = req.foerste ?: 20
            if (foerste !in 1..100 || req.etter?.isBlank() == true) {
                throw UgyldigForespørselException("foerste må være mellom 1 og 100, og etter kan ikke være tom.")
            }
            val fnr = dataSource.transaction(readOnly = true) { connection ->
                repositoryRegistry.provider(connection).provide<JournalpostRepository>()
                    .hentHvisEksisterer(Behandlingsreferanse(req.referanse))
                    ?.person?.aktivIdent()?.identifikator
                    ?: throw UgyldigForespørselException("Behandlingen mangler person.")
            }
            val brukerToken = token()
            val oversikt = gatewayProvider.provide<JournalpostOboGateway>()
                .hentAapJournalposter(fnr, foerste, req.etter, brukerToken)
            respond(oversikt.tilRespons { journalpostId ->
                TilgangGateway.harTilgangTilJournalpost(
                    JournalpostTilgangRequest(
                        journalpostId.toLong(), operasjon = Operasjon.SE, påkrevdRolle = emptyList()
                    ),
                    brukerToken
                ).tilgang
            })
        }
    }
}

internal suspend fun SafDokumentoversikt.tilRespons(
    kanLese: suspend (String) -> Boolean
): JournalposterRespons {
    val synlige = journalposter.filter {
        it.tema == "AAP" && it.skjerming == null && kanLese(it.journalpostId)
    }.map { journalpost ->
        JournalpostOversiktDto(
            journalpostId = journalpost.journalpostId,
            tittel = journalpost.tittel,
            dato = journalpost.datoOpprettet,
            tema = journalpost.tema,
            dokumenter = journalpost.dokumenter.filter { it.skjerming == null }.map { dokument ->
                JournalpostDokumentDto(
                    dokumentInfoId = dokument.dokumentInfoId,
                    tittel = dokument.tittel,
                    kanForhaandsvises = dokument.dokumentvarianter.any {
                        it.variantformat == SafVariantformat.ARKIV && it.saksbehandlerHarTilgang && it.skjerming == null
                    }
                )
            }
        )
    }
    return JournalposterRespons(synlige, sideInfo.sluttpeker, sideInfo.finnesNesteSide)
}
