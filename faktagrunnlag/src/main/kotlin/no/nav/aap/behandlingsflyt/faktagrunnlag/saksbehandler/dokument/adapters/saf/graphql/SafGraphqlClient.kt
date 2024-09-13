package no.nav.aap.behandlingsflyt.saf.graphql

import SafResponseHandler
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Dokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Filtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.JournalpostStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Variantformat
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.verdityper.dokument.DokumentInfoId
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI


interface SafGraphqlGateway {
    fun hentJournalpost(journalpostId: JournalpostId, currentToken: OidcToken? = null): Journalpost
}

class SafGraphqlClient(private val restClient: RestClient<InputStream>) : SafGraphqlGateway {
    private val log = LoggerFactory.getLogger(SafGraphqlClient::class.java)

    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.graphql"))

    companion object {
        private fun getClientConfig() = ClientConfig(
            scope = requiredConfigForKey("integrasjon.saf.scope"),
        )

        fun withClientCredentialsRestClient() =
            SafGraphqlClient(
                RestClient(
                    config = getClientConfig(),
                    tokenProvider = ClientCredentialsTokenProvider,
                    responseHandler = SafResponseHandler()
                )
            )

        fun withOboRestClient() =
            SafGraphqlClient(
                RestClient(
                    config = getClientConfig(),
                    tokenProvider = OnBehalfOfTokenProvider,
                    responseHandler = SafResponseHandler()
                )
            )
    }

    override fun hentJournalpost(journalpostId: JournalpostId, currentToken: OidcToken?): Journalpost {
        log.info("Henter journalpost: $journalpostId")
        val request = SafRequest.hentJournalpost(journalpostId)
        val response = runBlocking { graphqlQuery(request, currentToken) }

        val journalpost: SafJournalpost = response.data?.journalpost
            ?: error("Fant ikke journalpost for $journalpostId")

        val ident = when (journalpost.bruker?.type) {
            BrukerIdType.AKTOERID -> null //Ident.Aktørid(journalpost.bruker.id!!) //TODO: Må håndtere aktørid bittelitt mer fornuftig
            BrukerIdType.FNR -> Ident.Personident(journalpost.bruker.id!!)
            else -> null.also {
                log.warn("mottok noe annet enn a: ${journalpost.bruker?.type}")
            }
        }

        val mottattDato = journalpost.relevanteDatoer?.find { dato ->
            dato?.datotype == SafDatoType.DATO_REGISTRERT
        }?.dato?.toLocalDate() ?: error("Fant ikke dato")

        val dokumenter = journalpost.dokumenter?.filterNotNull()?.flatMap { dokument ->
            dokument.dokumentvarianter.filterNotNull().map { variant ->
                Dokument(
                    dokument.dokumentInfoId.let(::DokumentInfoId),
                    Variantformat.valueOf(variant.variantformat.name),
                    Filtype.valueOf(variant.filtype),
                    dokument.brevkode,
                    dokument.tittel
                )
            }
        } ?: emptyList()

        return if (ident == null) {
            Journalpost.UtenIdent(
                journalpostId = journalpost.journalpostId.let(::JournalpostId),
                status = finnJournalpostStatus(journalpost.journalstatus),
                journalførendeEnhet = journalpost.journalfoerendeEnhet,
                mottattDato = mottattDato,
                dokumenter = dokumenter
            )
        } else {
            Journalpost.MedIdent(
                personident = ident,
                journalpostId = journalpost.journalpostId.let(::JournalpostId),
                status = finnJournalpostStatus(journalpost.journalstatus),
                journalførendeEnhet = journalpost.journalfoerendeEnhet,
                mottattDato = mottattDato,
                dokumenter = dokumenter
            )
        }
    }

    private fun graphqlQuery(query: SafRequest, currentToken: OidcToken?): SafRespons {
        val request = PostRequest(query, currentToken = currentToken)
        return requireNotNull(restClient.post(uri = graphqlUrl, request))
    }

    private fun finnJournalpostStatus(status: Journalstatus?): JournalpostStatus {
        return when (status) {
            Journalstatus.MOTTATT -> JournalpostStatus.MOTTATT
            else -> JournalpostStatus.UKJENT
        }
    }
}