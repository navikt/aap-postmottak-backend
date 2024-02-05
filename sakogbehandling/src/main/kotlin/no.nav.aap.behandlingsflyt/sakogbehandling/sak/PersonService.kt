package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.aap.HttpClientFactory
import no.nav.aap.ktor.client.AzureAdTokenProvider
import no.nav.aap.ktor.client.AzureConfig
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.util.*

data class PdlConfig(
    val scope: String,
    val url: String
)

class PersonService(config: AzureConfig, pdlConfig: PdlConfig) {
    private val graphQLClient = GraphQLClient(config, pdlConfig)

    suspend fun hentPerson(ident: Ident): Person {
        val identliste = hentAlleIdenterForPerson(ident)
        return Person(1L, UUID.randomUUID(), identliste)
    }

    private suspend fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
        val response = graphQLClient.query<GraphQLRequest<IdentVariables>, PdlData>(
            GraphQLRequest(
                IDENT_QUERY,
                IdentVariables(ident.identifikator)
            )
        )

        fun onSuccess(resp: GraphQLResponse<PdlData>): List<Ident> {
            return resp.data?.hentIdenter?.identer?.filter {
                it.gruppe == PdlGruppe.FOLKEREGISTERIDENT // TODO: Skal vi bare slippe gjennom folkeregisteridenter?
            }?.map {
                Ident(identifikator = it.ident)
            } ?: emptyList()
        }

        fun onFailure(ex: Throwable): List<Ident> {
            // TODO: Logg ex eller deleger feil videre?
            return emptyList()
        }

        return response.fold(::onSuccess, ::onFailure)
    }
}

class GraphQLClient(config: AzureConfig, val pdlConfig: PdlConfig) {
    val httpClient = HttpClientFactory.createClient()
    val tokenProvider = AzureAdTokenProvider(config, pdlConfig.scope)

    suspend inline fun <reified Q : Any, R> query(query: Q): Result<GraphQLResponse<R>> {
        // TODO: Skal feil boble opp eller håndteres her?
        return runCatching {
            httpClient.post(pdlConfig.url) {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                bearerAuth(tokenProvider.getClientCredentialToken())
                header("TEMA", "AAP")
                setBody(query)
            }
        }.map {
            it.body<GraphQLResponse<R>>()
        }
    }
}

private const val ident = "\$ident"

private val IDENT_QUERY = """
    query($ident: ID!) {

        hentIdenter(ident: $ident, historikk: true) {
            identer {
                ident,
                historisk,
                gruppe
            }
        }
    }
""".trimIndent()

data class GraphQLRequest<T : Any>(
    val query: String,
    val variables: T
)

data class IdentVariables(
    val ident: String
)

data class GraphQLResponse<T>(
    val data: T?,
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?
)

data class PdlData(
    val hentIdenter: PdlIdenter?,
)

data class PdlIdenter(
    val identer: List<PdlIdent>
)

data class PdlIdent(
    val ident: String,
    val historisk: Boolean,
    val gruppe: PdlGruppe
)

enum class PdlGruppe {
    AKTORID, NPID, FOLKEREGISTERIDENT
}

data class GraphQLError(
    val message: String,
    val locations: List<GraphQLErrorLocation>,
    val path: List<String>?,
    val extensions: GraphQLErrorExtension
)

data class GraphQLErrorExtension(
    val code: String?,
    val classification: String
)

data class GraphQLErrorLocation(
    val line: Int?,
    val column: Int?
)

data class GraphQLExtensions(
    val warnings: List<GraphQLWarning>?
)

class GraphQLWarning(
    val query: String?,
    val id: String?,
    val code: String?,
    val message: String?,
    val details: String?,
)

