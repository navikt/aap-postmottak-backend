package no.nav.aap.postmottak.klient.pdl

import PdlResponseHandler
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.GeografiskTilknytning
import no.nav.aap.postmottak.gateway.GeografiskTilknytningOgAdressebeskyttelse
import no.nav.aap.postmottak.gateway.Navn
import no.nav.aap.postmottak.gateway.NavnMedIdent
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.klient.saf.graphql.SafGraphqlKlient
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
    private val restClient = RestClient(
        config = clientConfig,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = PdlResponseHandler(),
        prometheus = PrometheusProvider.prometheus
    )

    companion object : Factory<PdlGraphqlKlient> {
        override fun konstruer(): PdlGraphqlKlient {
            return PdlGraphqlKlient()
        }
    }

    override fun hentPersonBolk(
        personidenter: List<String>
    ): Map<String, NavnMedIdent?>? {
        val request = PdlRequest.hentPersonBolk(personidenter)
        val response = graphqlQuery(request)
        val data = response.data?.hentPersonBolk ?: return null

        return data.associateBy(
            { it.ident },
            {
                it.person?.let { p ->
                    NavnMedIdent(
                        p.navn.firstOrNull(),
                        p.folkeregisteridentifikator.firstOrNull()?.identifikasjonsnummer
                    )
                }
            })
    }

    override fun hentFødselsdato(personident: String): LocalDate? {
        val data = hentPerson(personident)
        return data?.foedselsdato?.firstOrNull { !it.metadata.historisk }?.foedselsdato
    }

    override fun hentNavn(personident: String): Navn? {
        val data = hentPerson(personident)
        return data?.navn?.firstOrNull()
    }

    private fun hentPerson(
        personident: String,

    ): HentPersonResult? {
        val request = PdlRequest.hentPerson(personident)
        val response = graphqlQuery(request)
        return response.data?.hentPerson
    }

    override fun hentGeografiskTilknytning(
        personident: String,
    ): GeografiskTilknytning? {
        val request = PdlRequest.hentGeografiskTilknytning(personident)
        val response = graphqlQuery(request)
        return response.data?.hentGeografiskTilknytning
    }

    override fun hentAdressebeskyttelseOgGeolokasjon(ident: Ident): GeografiskTilknytningOgAdressebeskyttelse {
        log.info("Henter adressebeskyttelse og geografisk tilknytning")
        val request = PdlRequest.hentAdressebeskyttelseOgGeografiskTilknytning(ident)
        val response = graphqlQuery(request)

        val data = response.data ?: error("Unexpected response from PDL: ${response.errors}")
        return GeografiskTilknytningOgAdressebeskyttelse(
            geografiskTilknytning = data.hentGeografiskTilknytning,
            adressebeskyttelse = data.hentPerson?.adressebeskyttelse.orEmpty()
        )
    }

    override fun hentAlleIdenterForPerson(ident: String): List<Ident> {
        val request = PdlRequest.hentAlleIdenterForPerson(ident)
        val response = graphqlQuery(request)

        return response.data
            ?.hentIdenter
            ?.identer
            ?.filter { it.gruppe == PdlGruppe.FOLKEREGISTERIDENT }
            ?.map { Ident(identifikator = it.ident, aktivIdent = it.historisk.not()) }
            .orEmpty()
    }

    private fun graphqlQuery(query: PdlRequest): PdlResponse {
        val request = PostRequest(
            query, additionalHeaders = listOf(
                Header("Accept", "application/json"),
                Header("TEMA", "AAP"),
                Header("Behandlingsnummer", BEHANDLINGSNUMMER_AAP_SAKSBEHANDLING)
            )
        )
        return requireNotNull(restClient.post(uri = graphqlUrl, request))
    }
}