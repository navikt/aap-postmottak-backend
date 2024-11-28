package no.nav.aap.postmottak.saf.graphql

import SafResponseHandler
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI
import kotlinx.coroutines.runBlocking

class SafGraphqlKlient(private val restClient: RestClient<InputStream>) {
    private val log = LoggerFactory.getLogger(SafGraphqlKlient::class.java)

    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.graphql"))

    companion object {
        private fun getClientConfig() = ClientConfig(
            scope = requiredConfigForKey("integrasjon.saf.scope"),
        )

        fun withClientCredentialsRestClient() =
            SafGraphqlKlient(
                RestClient(
                    config = getClientConfig(),
                    tokenProvider = ClientCredentialsTokenProvider,
                    responseHandler = SafResponseHandler()
                )
            )

        fun withOboRestClient() =
            SafGraphqlKlient(
                RestClient(
                    config = getClientConfig(),
                    tokenProvider = OnBehalfOfTokenProvider,
                    responseHandler = SafResponseHandler()
                )
            )
    }

    fun hentJournalpost(journalpostId: JournalpostId, currentToken: OidcToken? = null): SafJournalpost {
        log.info("Henter journalpost: $journalpostId")
        val request = SafRequest.hentJournalpost(journalpostId)
        val response = runBlocking { graphqlQuery(request, currentToken) }

        val journalpost: SafJournalpost = response.data?.journalpost
            ?: error("Fant ikke journalpost for $journalpostId")

        if (!listOf(BrukerIdType.FNR, BrukerIdType.AKTOERID).contains(journalpost.bruker?.type)) {
            log.warn("mottok noe annet enn aktør-id eller fnr: ${journalpost.bruker?.type}")
        }

        return journalpost
    }

    private fun graphqlQuery(query: SafRequest, currentToken: OidcToken?): SafRespons {
        val request = PostRequest(query, currentToken = currentToken)
        return requireNotNull(restClient.post(uri = graphqlUrl, request))
    }
}
