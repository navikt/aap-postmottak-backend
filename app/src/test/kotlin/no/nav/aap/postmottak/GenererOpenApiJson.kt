package no.nav.aap.postmottak

import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.server.engine.ConnectorType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureOBOTokenProvider
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import no.nav.aap.postmottak.test.FakeServers
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.InputStream
import java.net.URI
import java.nio.charset.StandardCharsets

private fun getToken(): OidcToken {
    val client = RestClient(
        config = ClientConfig(scope = "postmottak-backend"),
        tokenProvider = NoTokenTokenProvider(),
        responseHandler = DefaultResponseHandler()
    )
    return OidcToken(
        client.post<Unit, FakeServers.TestToken>(
            URI.create(requiredConfigForKey("NAIS_TOKEN_ENDPOINT")),
            PostRequest(Unit)
        )!!.access_token
    )
}

fun main() {
    FakeServers().start()
    val postgres = postgreSQLContainer()

    val dbConfig = DbConfig(
        url = postgres.jdbcUrl,
        username = postgres.username,
        password = postgres.password
    )

    val client: RestClient<InputStream> = RestClient(
        config = ClientConfig(scope = "postmottak-backend"),
        tokenProvider = AzureOBOTokenProvider,
        responseHandler = DefaultResponseHandler()
    )

    // Starter server
    val server = embeddedServer(Netty, port = 0) {
        val gatewayProvider = defaultGatewayProvider()
        server(dbConfig = dbConfig, postgresRepositoryRegistry, gatewayProvider)

        // Installerer også /test-ruta, i likhet med TestApp, slik at den blir med
        // i det genererte openapi.json-skjemaet.
        val datasource = initDatasource(dbConfig, SimpleMeterRegistry())
        apiRouting {
            simulerJournalpostHendelseApi(datasource, postgresRepositoryRegistry, gatewayProvider)
        }
    }.start()

    val port =
        runBlocking { server.engine.resolvedConnectors().first { it.type == ConnectorType.HTTP }.port }

    val openApiDoc =
        requireNotNull(
            client.get(
                URI.create("http://localhost:$port/openapi.json"),
                GetRequest(currentToken = getToken())
            ) { body, _ ->
                String(body.readAllBytes(), StandardCharsets.UTF_8)
            }
        )

    // Skriver til openapi.json i repo-roten, samme som ApiTest gjør i testene.
    val writer = BufferedWriter(FileWriter("../openapi.json"))
    writer.use {
        it.write(openApiDoc)
    }

    server.stop()
    postgres.stop()
}
