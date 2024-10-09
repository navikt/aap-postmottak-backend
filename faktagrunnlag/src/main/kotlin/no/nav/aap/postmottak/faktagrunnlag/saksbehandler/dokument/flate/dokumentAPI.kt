package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.SafRestClient
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.postmottak.saf.graphql.SafGraphqlClient
import no.nav.aap.postmottak.saf.graphql.SafVariantformat
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.sak.flate.DokumentResponsDTO
import no.nav.aap.postmottak.sakogbehandling.sak.flate.HentDokumentDTO
import no.nav.aap.postmottak.sakogbehandling.sak.flate.HentJournalpostDTO
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.verdityper.dokument.DokumentInfoId


fun NormalOpenAPIRoute.dokumentApi() {
    route("/api/dokumenter") {
        route("/{journalpostId}/{dokumentinfoId}") {
            get<HentDokumentDTO, DokumentResponsDTO> { req ->
                val journalpostId = req.journalpostId
                val dokumentInfoId = req.dokumentinfoId

                val token = token()
                val gateway = SafRestClient.withOboRestClient()
                val dokumentRespons =
                    gateway.hentDokument(
                        JournalpostId(journalpostId),
                        DokumentInfoId(dokumentInfoId),
                        SafVariantformat.ARKIV.name,
                        currentToken = token
                    )
                pipeline.context.response.headers.append(
                    name = "Content-Disposition", value = "inline; filename=${dokumentRespons.filnavn}"
                )

                respond(DokumentResponsDTO(stream = dokumentRespons.dokument))
            }
        }
        route("/{journalpostId}/info") {
            get<HentJournalpostDTO, DokumentInfoResponsDTO> { req ->
                val journalpostId = req.journalpostId

                val token = token()
                val journalpost =
                    SafGraphqlClient.withOboRestClient().hentJournalpost(JournalpostId(journalpostId), token)

                val ident = if (journalpost is Journalpost.MedIdent) DokumentIdent(
                    navn = "Navn Navnesen",
                    ident = journalpost.personident.id
                ) else null

                respond(
                    DokumentInfoResponsDTO(
                        ident,
                        dokumenter = journalpost.dokumenter().map { DokumentDto.fromDokument(it) }
                    )
                )
            }
        }
    }
}
