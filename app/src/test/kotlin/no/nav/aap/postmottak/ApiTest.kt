package no.nav.aap.postmottak

import io.ktor.server.engine.ConnectorType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.postmottak.faktagrunnlag.register.PersonRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.postmottak.test.fakes.vedlegg
import com.fasterxml.jackson.databind.ObjectMapper
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
import no.nav.aap.postmottak.test.Fakes
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.time.LocalDateTime
import java.nio.charset.StandardCharsets
import kotlin.test.fail

@Fakes
class ApiTest {

    companion object {
        private val postgres = postgreSQLContainer()
        private lateinit var port: Number

        private val dbConfig = DbConfig(
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )

        private val client: RestClient<InputStream> = RestClient(
            config = ClientConfig(scope = "postmottak-backend"),
            tokenProvider = AzureOBOTokenProvider,
            responseHandler = DefaultResponseHandler()
        )

        private var token: OidcToken? = null
        private fun getToken(): OidcToken {
            val client = RestClient(
                config = ClientConfig(scope = "postmottak-backend"),
                tokenProvider = NoTokenTokenProvider(),
                responseHandler = DefaultResponseHandler()
            )
            return token ?: OidcToken(
                client.post<Unit, FakeServers.TestToken>(
                    URI.create(requiredConfigForKey("NAIS_TOKEN_ENDPOINT")),
                    PostRequest(Unit)
                )!!.access_token
            )
        }

        // Starter server
        private val registre = postgresRepositoryRegistry
        private val gateways = defaultGatewayProvider()
        private val server = embeddedServer(Netty, port = 0) {
            server(dbConfig = dbConfig, registre, gateways)
        }

        @JvmStatic
        @BeforeAll
        fun beforeall() {
            server.start()
            port =
                runBlocking { server.engine.resolvedConnectors().first { it.type == ConnectorType.HTTP }.port }
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.stop()
            postgres.close()
        }
    }

    @Test
    fun `journalpostlisten henter personens AAP-journalposter fra SAF med paginering og metadataautorisasjon`() {
        val kilde = TestJournalposter.leggTil { mottattTid = LocalDateTime.of(2026, 1, 1, 12, 0) }
        val annen = TestJournalposter.leggTil {
            fnr = kilde.brukerId
            mottattTid = LocalDateTime.of(2026, 2, 1, 12, 0)
            dokumenter = listOf(vedlegg("Ettersendt legeerklæring"))
        }
        TestJournalposter.leggTil { fnr = kilde.brukerId; tema = "DAG" }
        val annenPerson = TestJournalposter.leggTil {}
        initDatasource(dbConfig, SimpleMeterRegistry()).use { db ->
            val behandling = db.transaction {
                val repositories = registre.provider(it)
                val journalpost = kilde.tilJournalpost()
                val person = repositories.provide<PersonRepository>().finnEllerOpprett(journalpost.person.identer())
                repositories.provide<JournalpostRepository>().lagre(journalpost.copy(person = person))
                val behandlinger = repositories.provide<BehandlingRepository>()
                behandlinger.hent(behandlinger.opprettBehandling(kilde.journalpostId(), TypeBehandling.Journalføring))
            }
            val uri = "http://localhost:$port/api/behandling/${behandling.referanse.referanse}/journalposter"
            val http = HttpClient.newHttpClient()
            fun hent(query: String = "", ident: String = "Journalpostleser"): HttpResponse<String> =
                http.send(
                    HttpRequest.newBuilder(URI(uri + query))
                        .header("Authorization", "Bearer ${FakeServers.saksbehandlerToken(ident)}").GET().build(),
                    HttpResponse.BodyHandlers.ofString()
                )

            val første = hent("?foerste=1")
            assertEquals(200, første.statusCode(), første.body())
            val side = ObjectMapper().readTree(første.body())
            val journalpost = side.required("journalposter").single()
            assertEquals(annen.journalpostId.toString(), journalpost.required("journalpostId").asText())
            assertEquals("AAP", journalpost.required("tema").asText())
            assertEquals("2026-02-01T12:00:00", journalpost.required("dato").asText())
            assertEquals("Ettersendt legeerklæring", journalpost.required("dokumenter").single().required("tittel").asText())
            assertTrue(side.required("finnesNesteSide").booleanValue())
            assertFalse(første.body().contains(annenPerson.journalpostId.toString()))

            val neste = hent("?foerste=1&etter=${side.required("sluttpeker").asText()}")
            assertEquals(200, neste.statusCode(), neste.body())
            val nesteSide = ObjectMapper().readTree(neste.body())
            assertEquals(kilde.journalpostId.toString(), nesteSide.required("journalposter").single().required("journalpostId").asText())
            assertFalse(nesteSide.required("finnesNesteSide").booleanValue())
            assertEquals(400, hent("?foerste=0").statusCode())
            assertEquals(400, hent("?foerste=101").statusCode())
            assertEquals(400, hent("?etter=").statusCode())

            FakeServers.journalposterUtenLesetilgang.add(annen.journalpostId)
            try {
                val begrenset = hent("?foerste=1", "BegrensetOversikt${annen.journalpostId}")
                assertEquals(200, begrenset.statusCode(), begrenset.body())
                val skjermetSide = ObjectMapper().readTree(begrenset.body())
                assertEquals(0, skjermetSide.required("journalposter").size())
                assertTrue(skjermetSide.required("finnesNesteSide").booleanValue())
                assertFalse(begrenset.body().contains("Ettersendt legeerklæring"))
                assertFalse(begrenset.body().contains(annen.journalpostId.toString()))
            } finally {
                FakeServers.journalposterUtenLesetilgang.remove(annen.journalpostId)
            }
            FakeServers.journalposterUtenLesetilgang.add(kilde.journalpostId)
            try {
                val avvist = hent(ident = "IngenBehandlingstilgang${kilde.journalpostId}")
                assertEquals(403, avvist.statusCode(), avvist.body())
                assertFalse(avvist.body().contains("Ettersendt legeerklæring"))
            } finally {
                FakeServers.journalposterUtenLesetilgang.remove(kilde.journalpostId)
            }
        }
    }

    @Test
    fun `skal lagre openapi som fil`() {
        val openApiDoc =
            requireNotNull(
                client.get(
                    URI.create("http://localhost:$port/openapi.json"),
                    GetRequest(currentToken = getToken())
                ) { body, _ ->
                    String(body.readAllBytes(), StandardCharsets.UTF_8)
                }
            )

        try {
            val writer = BufferedWriter(FileWriter("../openapi.json"))
            writer.write(openApiDoc)

            writer.close()
        } catch (_: Exception) {
            fail()
        }

    }

}