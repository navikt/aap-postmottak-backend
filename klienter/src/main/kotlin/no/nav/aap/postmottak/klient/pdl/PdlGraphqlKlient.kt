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
import no.nav.aap.postmottak.saf.graphql.SafGraphqlKlient
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI

class PdlGraphqlKlient(
    private val restClient: RestClient<InputStream>
) {
    private val log = LoggerFactory.getLogger(SafGraphqlKlient::class.java)

    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.pdl.url")).resolve("/graphql")

    companion object {
        private fun getClientConfig() = ClientConfig(
            scope = requiredConfigForKey("integrasjon.pdl.scope"),
        )
        private const val BEHANDLINGSNUMMER_AAP_SAKSBEHANDLING = "B287"

        fun withClientCredentialsRestClient() =
            PdlGraphqlKlient(
                RestClient(
                    config = getClientConfig(),
                    tokenProvider = ClientCredentialsTokenProvider,
                    responseHandler = PdlResponseHandler()
                )
            )

        fun withOboRestClient() =
            PdlGraphqlKlient(
                RestClient(
                    config = getClientConfig(),
                    tokenProvider = OnBehalfOfTokenProvider,
                    responseHandler = PdlResponseHandler()
                )
            )
    }

    fun hentPersonBolk(
        personidenter: List<String>,
        currentToken: OidcToken? = null
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
    
    fun hentGeografiskTilknytning(
        personident: String,
        currentToken: OidcToken? = null
    ): GeografiskTilknytning? {
        val request = PdlRequest.hentGeografiskTilknytning(personident)
        val response = runBlocking { graphqlQuery(request, currentToken) }
        return response.data?.hentGeografiskTilknytning
    }

    fun hentAdressebeskyttelseOgGeolokasjon(ident: Ident, currentToken: OidcToken? = null): PdlData {
        log.info("Henter adressebeskyttelse og geografisk tilknytning")
        val request = PdlRequest.hentAdressebeskyttelseOgGeografiskTilknytning(ident)
        val response = runBlocking { graphqlQuery(request, currentToken) }

        return response.data ?: error("Unexpected response from PDL: ${response.errors}")
    }

    fun hentAlleIdenterForPerson(ident: String, currentToken: OidcToken? = null): List<Ident> {
        val request = PdlRequest.hentAlleIdenterForPerson(ident)
        val response = runBlocking { graphqlQuery(request, currentToken) }
        
        return response.data
            ?.hentIdenter
            ?.identer
            ?.filter { it.gruppe == PdlGruppe.FOLKEREGISTERIDENT }
            ?.map { Ident(identifikator = it.ident, aktivIdent = it.historisk.not()) }
            ?: emptyList()
    }
    
    private fun graphqlQuery(query: PdlRequest, currentToken: OidcToken? = null): PdlResponse {
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