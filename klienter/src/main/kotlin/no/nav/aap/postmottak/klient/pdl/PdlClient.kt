package no.nav.aap.postmottak.klient.pdl

import PdlResponseHandler
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.postmottak.saf.graphql.SafGraphqlClient
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI

interface IPdlGraphQLClient {
    fun hentPersonBolk(personidenter: List<String>, currentToken: OidcToken? = null): List<HentPersonBolkResult>?
}

class PdlGraphQLClient(
    private val restClient: RestClient<InputStream>
) : IPdlGraphQLClient {
    private val log = LoggerFactory.getLogger(SafGraphqlClient::class.java)

    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.pdl.url")).resolve("/graphql")

    companion object {
        private fun getClientConfig() = ClientConfig(
            scope = requiredConfigForKey("integrasjon.pdl.scope"),
        )
        private const val BEHANDLINGSNUMMER_AAP_SAKSBEHANDLING = "B287"

        fun withClientCredentialsRestClient() =
            PdlGraphQLClient(
                RestClient(
                    config = getClientConfig(),
                    tokenProvider = ClientCredentialsTokenProvider,
                    responseHandler = PdlResponseHandler()
                )
            )

        fun withOboRestClient() =
            PdlGraphQLClient(
                RestClient(
                    config = getClientConfig(),
                    tokenProvider = OnBehalfOfTokenProvider,
                    responseHandler = PdlResponseHandler()
                )
            )
    }

    override fun hentPersonBolk(
        personidenter: List<String>,
        currentToken: OidcToken?
    ): List<HentPersonBolkResult>? {
        val request = PdlRequest.hentPersonBolk(personidenter)
        val response = runBlocking { graphqlQuery(request, currentToken) }
        return response.data?.hentPersonBolk
    }
    
    fun hentPerson(
        personident: String,
        currentToken: OidcToken? = null
    ): HentPersonResult? {
        val request = PdlRequest.hentPerson(personident)
        val response = runBlocking { graphqlQuery(request, currentToken) }
        return response.data?.hentPerson
    }
    
    private fun graphqlQuery(query: PdlRequest, currentToken: OidcToken?): PdlResponse {
        val request = PostRequest(
            query, currentToken = currentToken, additionalHeaders = listOf(
                Header("Accept", "application/json"),
                Header("TEMA", "AAP"),
                Header("Behandlingsnummer", BEHANDLINGSNUMMER_AAP_SAKSBEHANDLING)
            )
        )
        return requireNotNull(restClient.post(uri = graphqlUrl, request))
    }
}