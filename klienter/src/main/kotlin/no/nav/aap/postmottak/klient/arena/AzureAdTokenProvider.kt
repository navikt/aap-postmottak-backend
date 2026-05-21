package no.nav.aap.postmottak.klient.arena

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.aap.komponenter.config.requiredConfigForKey

class AzureAdTokenProvider(private val client: HttpClient) {
    val endpoint = requiredConfigForKey("nais.token.endpoint")
    suspend fun getClientCredentialToken(scope: String): String = client.post(endpoint) {
        accept(ContentType.Application.Json)
        contentType(ContentType.Application.Json)
        setBody(
            mapOf(
                "identity_provider" to "entra_id",
                "target" to scope,
            )
        )
    }.body<JsonNode>().get("access_token").asText()
}