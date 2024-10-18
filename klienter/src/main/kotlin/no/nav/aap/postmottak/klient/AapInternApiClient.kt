package no.nav.aap.postmottak.klient

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import java.net.URI

class AapInternApiClient {
    private val url = URI.create(requiredConfigForKey("integrasjon.aap.intern.api.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.aap.intern.api.scope"),
    )
    private val client =
        RestClient.withDefaultResponseHandler(config = config, tokenProvider = ClientCredentialsTokenProvider)

    fun hentSakerForIdent(ident: String): List<ArenaSak> {
        val path = url.resolve("/api/v1/sakerByFnr")
        val reqbody = mapOf("personidentifikator" to ident)
        return client.post(path, PostRequest(body = reqbody), mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        })!!
    }
}

data class ArenaSak(val saksnummer: String, val periode: Periode)