package no.nav.aap.postmottak.klient.pdl

import PdlResponseHandler
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.Navn
import no.nav.aap.postmottak.gateway.PersondataOboGateway
import org.slf4j.LoggerFactory
import java.net.URI

private const val BEHANDLINGSNUMMER_AAP_SAKSBEHANDLING = "B287"

class PdlOboGraphqlKlient : PersondataOboGateway {
    private val log = LoggerFactory.getLogger(PdlOboGraphqlKlient::class.java)

    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.pdl.url")).resolve("/graphql")
    private val clientConfig = ClientConfig(
        scope = requiredConfigForKey("integrasjon.pdl.scope"),
    )
    private val restClient = RestClient(
        config = clientConfig,
        tokenProvider = OnBehalfOfTokenProvider,
        responseHandler = PdlResponseHandler(),
        prometheus = PrometheusProvider.prometheus
    )

    companion object : Factory<PdlOboGraphqlKlient> {
        override fun konstruer(): PdlOboGraphqlKlient {
            return PdlOboGraphqlKlient()
        }
    }

    override fun hentNavn(personident: String, currentToken: OidcToken): Navn? {
        val data = hentPerson(personident, currentToken)
        return data?.navn?.firstOrNull()
    }

    private fun hentPerson(personident: String, currentToken: OidcToken): HentPersonResult? {
        val request = PdlRequest.hentPerson(personident)
        val response = graphqlQuery(request, currentToken)
        return response.data?.hentPerson
    }

    private fun graphqlQuery(query: PdlRequest, currentToken: OidcToken): PdlResponse {
        val request = PostRequest(
            query, 
            currentToken = currentToken,
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
                Header("TEMA", "AAP"),
                Header("Behandlingsnummer", BEHANDLINGSNUMMER_AAP_SAKSBEHANDLING)
            )
        )
        return requireNotNull(restClient.post(uri = graphqlUrl, request))
    }
}

