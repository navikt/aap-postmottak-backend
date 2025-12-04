package no.nav.aap.postmottak.klient.saf.graphql

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.JournalpostOboGateway
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.SafSak
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.saf.graphql.SafRespons
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI

class SafGraphqlOboClient : JournalpostOboGateway {
    private val scope = requiredConfigForKey("integrasjon.saf.scope")
    val safGraphqlKlient = SafGraphqlKlient(
        RestClient(
            config = ClientConfig(scope),
            OnBehalfOfTokenProvider,
            responseHandler = SafResponseHandler(),
            prometheus = PrometheusProvider.prometheus
        )
    )

    companion object : Factory<SafGraphqlOboClient> {
        override fun konstruer(): SafGraphqlOboClient {
            return SafGraphqlOboClient()
        }
    }

    override fun hentJournalpost(journalpostId: JournalpostId, currentToken: OidcToken): SafJournalpost =
        safGraphqlKlient.hentJournalpostInternal(journalpostId, currentToken)

    override fun hentSaker(fnr: String, currentToken: OidcToken): List<SafSak> =
        safGraphqlKlient.hentSakerInternal(fnr, currentToken)

}

class SafGraphqlClientCredentialsClient : JournalpostGateway {
    private val scope = requiredConfigForKey("integrasjon.saf.scope")

    val safGraphqlKlient = SafGraphqlKlient(
        RestClient(
            config = ClientConfig(scope),
            ClientCredentialsTokenProvider,
            responseHandler = SafResponseHandler(),
            prometheus = PrometheusProvider.prometheus
        )
    )

    companion object : Factory<SafGraphqlClientCredentialsClient> {
        override fun konstruer(): SafGraphqlClientCredentialsClient {
            return SafGraphqlClientCredentialsClient()
        }

    }

    override fun hentJournalpost(journalpostId: JournalpostId) =
        safGraphqlKlient.hentJournalpostInternal(journalpostId, null)

    override fun hentSaker(fnr: String) = safGraphqlKlient.hentSakerInternal(fnr, null)
}

class SafGraphqlKlient(val restClient: RestClient<InputStream>) {
    private val log = LoggerFactory.getLogger(SafGraphqlKlient::class.java)
    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.graphql"))


    fun hentJournalpostInternal(journalpostId: JournalpostId, currentToken: OidcToken?): SafJournalpost {
        log.info("Henter journalpost: $journalpostId")
        val request = SafRequest.hentJournalpost(journalpostId)
        val response = graphqlQuery(request, currentToken)

        val journalpost: SafJournalpost = response.data?.journalpost
            ?: error("Fant ikke journalpost for $journalpostId")

        if (!listOf(BrukerIdType.FNR, BrukerIdType.AKTOERID).contains(journalpost.bruker?.type)) {
            log.warn("mottok noe annet enn aktør-id eller fnr: ${journalpost.bruker?.type}")
        }

        return journalpost
    }

    fun hentSakerInternal(fnr: String, currentToken: OidcToken? = null): List<SafSak> {
        val request = SafRequest.hentSaker(fnr)
        val response = graphqlQuery(request, currentToken)
        val saker: List<SafSak> = response.data?.saker ?: emptyList()
        return saker
    }

    private fun graphqlQuery(query: SafRequest, currentToken: OidcToken?): SafRespons {
        val request = PostRequest(query, currentToken = currentToken)
        return requireNotNull(restClient.post(uri = graphqlUrl, request))
    }
}
