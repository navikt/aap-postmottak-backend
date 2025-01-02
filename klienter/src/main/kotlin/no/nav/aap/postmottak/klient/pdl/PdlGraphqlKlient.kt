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
import no.nav.aap.lookup.gateway.Factory
import no.nav.aap.postmottak.gateway.GeografiskTilknytning
import no.nav.aap.postmottak.gateway.GeografiskTilknytningOgAdressebeskyttelse
import no.nav.aap.postmottak.gateway.Navn
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.saf.graphql.SafGraphqlKlient
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate

private const val BEHANDLINGSNUMMER_AAP_SAKSBEHANDLING = "B287"

class PdlGraphqlKlient : PersondataGateway {
    private val log = LoggerFactory.getLogger(SafGraphqlKlient::class.java)

    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.pdl.url")).resolve("/graphql")
    private val clientConfig = ClientConfig(
        scope = requiredConfigForKey("integrasjon.pdl.scope"),
    )
    val restClient = RestClient(
        config = clientConfig,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = PdlResponseHandler()
    )

    companion object: Factory<PdlGraphqlKlient> {
        override fun konstruer(): PdlGraphqlKlient {
            return PdlGraphqlKlient()
        }
    }

    override fun hentPersonBolk(
        personidenter: List<String>
    ): Map<String, Navn>? {
        val request = PdlRequest.hentPersonBolk(personidenter)
        val response = runBlocking { graphqlQuery(request, null) }
        val data = response.data?.hentPersonBolk
        if (data == null) {
            return null
        }

        return data.associateBy({ it.ident }, { Navn(it.person?.navn?.first()?.fulltNavn()) })
    }

    override fun hentFødselsdato(personident: String): LocalDate? {
        val data = hentPerson(personident)
        return data?.foedselsdato?.first { !it.metadata.historisk }?.foedselsdato
    }

    private fun hentPerson(
        personident: String,
        currentToken: OidcToken? = null
    ): HentPersonResult? {
        val request = PdlRequest.hentPerson(personident)
        val response = runBlocking { graphqlQuery(request, currentToken) }
        return response.data?.hentPerson
    }

    override fun hentGeografiskTilknytning(
        personident: String,
    ): GeografiskTilknytning? {
        val request = PdlRequest.hentGeografiskTilknytning(personident)
        val response = runBlocking { graphqlQuery(request, null) }
        return response.data?.hentGeografiskTilknytning
    }

    override fun hentAdressebeskyttelseOgGeolokasjon(ident: Ident): GeografiskTilknytningOgAdressebeskyttelse {
        log.info("Henter adressebeskyttelse og geografisk tilknytning")
        val request = PdlRequest.hentAdressebeskyttelseOgGeografiskTilknytning(ident)
        val response = runBlocking { graphqlQuery(request, null) }

        val data =  response.data ?: error("Unexpected response from PDL: ${response.errors}")
        return GeografiskTilknytningOgAdressebeskyttelse(
            geografiskTilknytning = data.hentGeografiskTilknytning ?: error("Geografisk tilknytning mangler"),
            adressebeskyttelse = data.hentPerson?.adressebeskyttelse ?: emptyList()
        )
    }

    override fun hentAlleIdenterForPerson(ident: String): List<Ident> {
        return hentAlleIdenterForPerson(ident, null)
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