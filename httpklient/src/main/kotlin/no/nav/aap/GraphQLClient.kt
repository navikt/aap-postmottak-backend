package no.nav.aap

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

abstract class GraphQLClient(
    @PublishedApi internal val config: GraphQLConfig,
    @PublishedApi internal val client: HttpClient = HttpClientFactory.createClient(),
) {
    suspend inline fun <reified T : Any, reified R : Any> query(
        req: T,
    ): Result<R> {
        return runCatching {
            client.post(config.url) {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                bearerAuth(getToken())
                additionalHeaders(config.additionalHeaders)
                setBody<T>(req)
            }
        }.map {
            it.body<R>()
        }
    }

    abstract suspend fun getToken(): String

    @PublishedApi
    internal fun HttpMessageBuilder.additionalHeaders(
        headers: List<Pair<String, String>>,
    ) {
        headers.map { (key, value) ->
            header(key, value)
        }
    }
}

open class GraphQLConfig(
    val url: String,
    val additionalHeaders: List<Pair<String, String>> = emptyList()
)
