package no.nav.aap.postmottak.klient.saf

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.lookup.gateway.Factory
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.DokumentGateway
import no.nav.aap.postmottak.gateway.DokumentOboGateway
import no.nav.aap.postmottak.gateway.SafDocumentResponse
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.io.InputStream
import java.net.URI
import java.net.http.HttpHeaders

class SafOboRestClient(client: RestClient<InputStream>) : SafRestKlient(client), DokumentOboGateway {
    companion object : Factory<SafOboRestClient> {
        override fun konstruer(): SafOboRestClient {
            val client = RestClient.withDefaultResponseHandler(
                config = ClientConfig(
                    scope = requiredConfigForKey("integrasjon.saf.scope"),
                ),
                OnBehalfOfTokenProvider,
                prometheus = PrometheusProvider.prometheus
            )
            return SafOboRestClient(client)
        }
    }

    override fun hentDokument(
        journalpostId: JournalpostId,
        dokumentId: DokumentInfoId,
        arkivtype: String,
        currentToken: OidcToken
    ) = hentDokumentInternal(client, journalpostId, dokumentId, arkivtype, currentToken)

}

class SafRestClient(client: RestClient<InputStream>) : SafRestKlient(client), DokumentGateway {
    companion object : Factory<SafRestClient> {
        override fun konstruer(): SafRestClient {
            val client = RestClient.withDefaultResponseHandler(
                config = ClientConfig(
                    scope = requiredConfigForKey("integrasjon.saf.scope"),
                ),
                tokenProvider = ClientCredentialsTokenProvider,
                prometheus = PrometheusProvider.prometheus
            )
            return SafRestClient(client)
        }

        fun konstruer(client: RestClient<InputStream>): SafRestClient {
            return SafRestClient(client)
        }
    }

    override fun hentDokument(
        journalpostId: JournalpostId,
        dokumentId: DokumentInfoId,
        arkivtype: String,
    ) = hentDokumentInternal(client, journalpostId, dokumentId, arkivtype)
}

abstract class SafRestKlient(val client: RestClient<InputStream>) {
    private val restUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.rest"))
    val scope = requiredConfigForKey("integrasjon.saf.scope")


    protected fun hentDokumentInternal(
        client: RestClient<InputStream>,
        journalpostId: JournalpostId,
        dokumentId: DokumentInfoId,
        arkivtype: String,
        currentToken: OidcToken? = null,
    ): SafDocumentResponse {
        val url = konstruerSafRestURL(restUrl, journalpostId, dokumentId, arkivtype)
        val response = client.get(
            url, request = GetRequest(currentToken = currentToken),
            mapper = { body, headers ->
                val contentType = headers.map()["Content-Type"]?.firstOrNull()
                val filnavn: String? = extractFileNameFromHeaders(headers)

                if (contentType == null || filnavn == null) {
                    throw IllegalStateException("Respons inneholdt ikke korrekte headere: $headers")
                }
                SafDocumentResponse(dokument = body, contentType = contentType, filnavn = filnavn)
            })
        return response!!
    }

    private fun konstruerSafRestURL(
        baseUrl: URI,
        journalpostId: JournalpostId,
        dokumentInfoId: DokumentInfoId,
        variantFormat: String
    ): URI {
        return URI.create("$baseUrl/hentdokument/${journalpostId.referanse}/${dokumentInfoId.dokumentInfoId}/${variantFormat}")
    }

    private fun extractFileNameFromHeaders(headers: HttpHeaders): String? {
        val value = headers.map()["Content-Disposition"]?.firstOrNull()
        if (value.isNullOrBlank()) {
            return null
        }
        val regex =
            Regex("filename=([^;]+)")

        val matchResult = regex.find(value)
        return matchResult?.groupValues?.get(1)
    }
}
