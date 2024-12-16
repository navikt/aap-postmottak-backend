package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.postmottak.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.klient.pdl.PdlGraphqlKlient
import no.nav.aap.postmottak.klient.saf.SafRestKlient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.saf.graphql.SafGraphqlKlient
import no.nav.aap.postmottak.saf.graphql.SafVariantformat
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.sakogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.sakogbehandling.sak.flate.DokumentResponsDTO
import no.nav.aap.postmottak.sakogbehandling.sak.flate.HentDokumentDTO
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource


fun NormalOpenAPIRoute.dokumentApi(dataSource: DataSource) {
    route("/api/dokumenter") {
        route("/{journalpostId}/{dokumentinfoId}") {
            authorizedGet<HentDokumentDTO, DokumentResponsDTO>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "journalpostId"
                    )
                )
            ) { req ->
                val journalpostId = req.journalpostId
                val dokumentInfoId = req.dokumentinfoId

                val token = token()
                val gateway = SafRestKlient.withOboRestClient()
                val dokumentRespons =
                    gateway.hentDokument(
                        JournalpostId(journalpostId),
                        DokumentInfoId(dokumentInfoId),
                        SafVariantformat.ARKIV.name,
                        currentToken = token
                    )
                pipeline.call.response.headers.append(
                    name = "Content-Disposition", value = "inline; filename=${dokumentRespons.filnavn}"
                )

                respond(DokumentResponsDTO(stream = dokumentRespons.dokument))
            }
        }

        route("/{referanse}/info") {
            authorizedGet<BehandlingsreferansePathParam, DokumentInfoResponsDTO>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "referanse",
                        journalpostIdFraBehandlingResolver(dataSource)
                    )
                )
            ) { req ->
                val journalpostId = dataSource.transaction(readOnly = true) { connection ->
                    BehandlingRepositoryImpl(connection).hent(req).journalpostId
                }

                val token = token()
                val journalpost =
                    SafGraphqlKlient.withOboRestClient().hentJournalpost(JournalpostId(journalpostId.referanse), token)
                val identer =
                    listOf(journalpost.bruker?.id, journalpost.avsenderMottaker?.id).filterNotNull().distinct()
                val personer = PdlGraphqlKlient.withClientCredentialsRestClient().hentPersonBolk(identer)
                respond(
                    DokumentInfoResponsDTO(
                        journalpostId = journalpostId.referanse,
                        søker = DokumentIdent(
                            journalpost.bruker?.id,
                            personer?.find { it.ident == journalpost.bruker?.id }?.person?.navn?.first()?.fulltNavn()
                        ),
                        avsender = DokumentIdent(
                            journalpost.avsenderMottaker?.id,
                            personer?.find { it.ident == journalpost.avsenderMottaker?.id }?.person?.navn?.first()
                                ?.fulltNavn()
                        ),
                        dokumenter = journalpost.dokumenter?.mapNotNull { DokumentDto.fromDokument(it!!) }
                            ?: emptyList()
                    )
                )
            }
        }
    }
}
