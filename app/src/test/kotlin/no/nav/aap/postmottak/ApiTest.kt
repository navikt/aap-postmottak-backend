package no.nav.aap.postmottak

import io.ktor.server.engine.ConnectorType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.gateway.DokarkivGateway
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.test.fakes.InMemoryUnder18Repository
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.postmottak.test.fakes.vedlegg
import no.nav.aap.postmottak.under18.Under18Søknadsgrunnlag
import no.nav.aap.postmottak.under18.Under18SamtykkeVurdering
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
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
        private val registre = postgresRepositoryRegistry.register<InMemoryUnder18Repository>()
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
    fun `Under18 viser dokumentbehandlingen uten å endre versjon og avviser hele svaret ved manglende tilgang`() {
        val kilde = TestJournalposter.leggTil {
            dokumenter = listOf(vedlegg("Søknad").copy(dokumentInfoId = DokumentInfoId("482171")))
        }.tilJournalpost()
        val sak = "L1-test"
        val samtykke = TestJournalposter.leggTil {}.journalpostId()
        initDatasource(dbConfig, SimpleMeterRegistry()).use { db ->
            val under18 = db.transaction {
                val provider = registre.provider(it)
                val behandlinger = provider.provide<BehandlingRepository>()
                val id = behandlinger.opprettBehandling(kilde.journalpostId, TypeBehandling.Under18)
                InMemoryUnder18Repository.lagreSøknadsgrunnlag(id, Under18Søknadsgrunnlag(kilde.person.id, listOf(kilde.journalpostId)))
                InMemoryUnder18Repository.lagreSamtykkevurdering(id, Under18SamtykkeVurdering(true, listOf(samtykke)))
                provider.provide<SaksnummerRepository>().lagreSakVurdering(id, Saksvurdering(sak))
                behandlinger.hent(id)
            }
            val uri = URI("http://localhost:$port/api/behandling/${under18.referanse.referanse}/grunnlag/under18")
            val http = HttpClient.newHttpClient()
            fun hent(ident: String): HttpResponse<String> {
                val request = HttpRequest.newBuilder(uri)
                    .header("Authorization", "Bearer ${FakeServers.saksbehandlerToken(ident)}").GET().build()
                return http.send(request, HttpResponse.BodyHandlers.ofString())
            }
            val utenKobling = hent("Leser")
            assertEquals(200, utenKobling.statusCode(), utenKobling.body())
            assertFalse(ObjectMapper().readTree(utenKobling.body()).required("søknader").single().hasNonNull("tilknyttetJournalpostId"))

            val ny = gateways.provide<DokarkivGateway>().knyttTilAnnenSak(kilde, sak, "9999")
            val dokument = db.transaction {
                val behandlinger = registre.provider(it).provide<BehandlingRepository>()
                behandlinger.hent(behandlinger.opprettBehandling(ny, TypeBehandling.DokumentHåndtering))
            }
            repeat(2) {
                val svar = hent("Leser")
                assertEquals(200, svar.statusCode(), svar.body())
                val json = ObjectMapper().readTree(svar.body())
                assertEquals(under18.versjon, json.required("behandling").required("versjon").longValue())
                val søknad = json.required("søknader").single()
                assertEquals(ny.referanse, søknad.required("tilknyttetJournalpostId").longValue())
                assertEquals(dokument.referanse.referanse.toString(), søknad.required("dokumentbehandling").required("referanse").textValue())
            }
            for (skjermet in listOf(ny, samtykke)) {
                FakeServers.journalposterUtenLesetilgang.add(skjermet.referanse)
                try {
                    val svar = hent("Begrenset${skjermet.referanse}")
                    assertEquals(403, svar.statusCode())
                    assertFalse(svar.body().contains(skjermet.referanse.toString()))
                    assertFalse(svar.body().contains(dokument.referanse.referanse.toString()))
                } finally {
                    FakeServers.journalposterUtenLesetilgang.remove(skjermet.referanse)
                }
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