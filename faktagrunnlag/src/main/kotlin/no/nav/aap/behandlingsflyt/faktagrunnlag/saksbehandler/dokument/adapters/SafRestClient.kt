package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.verdityper.dokument.DokumentInfoId
import java.io.InputStream
import java.net.URI
import java.net.http.HttpHeaders

class SafRestClient(private val restClient: RestClient<InputStream>) {

    private val restUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.rest"))
    
    companion object {
        val config = ClientConfig(
            scope = requiredConfigForKey("integrasjon.saf.scope"),
        )
        fun withDefaultRestClient(): SafRestClient {
            return SafRestClient(
                RestClient.withDefaultResponseHandler(
                    config = config,
                    tokenProvider = OnBehalfOfTokenProvider
                )
            )
        }
    }

    fun hentDokument(
        journalpostId: JournalpostId,
        dokumentId: DokumentInfoId,
        arkivtype: String = "ORIGINAL",
        currentToken: OidcToken,
    ): SafDocumentResponse {
        val url = konstruerSafRestURL(restUrl, journalpostId, dokumentId, arkivtype)
        val response = restClient.get(url,request = GetRequest(currentToken = currentToken),
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
data class SafDocumentResponse(val dokument: InputStream, val contentType: String, val filnavn: String)