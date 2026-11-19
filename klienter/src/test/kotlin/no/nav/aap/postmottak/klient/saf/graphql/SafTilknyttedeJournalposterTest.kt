package no.nav.aap.postmottak.klient.saf.graphql

import com.sun.net.httpserver.HttpServer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.SafTilknyttetDokument
import no.nav.aap.postmottak.gateway.SafTilknyttetJournalpost
import no.nav.aap.postmottak.gateway.Sak
import no.nav.aap.postmottak.gateway.Sakstype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.klient.graphql.ErrorCode
import no.nav.aap.postmottak.klient.graphql.GraphQLError
import no.nav.aap.postmottak.klient.graphql.GraphQLErrorExtension
import no.nav.aap.postmottak.klient.graphql.GraphQLExtensions
import no.nav.aap.postmottak.klient.graphql.GraphQLWarning
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.atomic.AtomicReference

class SafTilknyttedeJournalposterTest {
    @BeforeEach
    fun setUp() {
        PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    @Test
    fun `sender dokument-ID som variabel og returnerer sak og komplett dokumentsett`() {
        val respons = safRespons(listOf(
            SafTilknyttetJournalpost("123", null, listOf(SafTilknyttetDokument("456"))),
            SafTilknyttetJournalpost(
                "789",
                Sak("SAK1", "KELVIN", Sakstype.FAGSAK),
                listOf(SafTilknyttetDokument("456"), SafTilknyttetDokument("457"))
            )
        ))

        medSafRespons(respons) { klient ->
            val resultat = klient.hentTilknyttedeJournalposterInternal(DokumentInfoId("456"), null)

            assertThat(resultat.map { it.journalpostId }).containsExactly("123", "789")
            assertThat(resultat.first().sak).isNull()
            assertThat(resultat.last().sak).isEqualTo(Sak("SAK1", "KELVIN", Sakstype.FAGSAK))
            assertThat(resultat.last().dokumenter.map { it.dokumentInfoId }).containsExactly("456", "457")
        }
    }

    @Test
    fun `tom liste er et gyldig resultat`() {
        medSafRespons(safRespons()) { klient ->
            assertThat(klient.hentTilknyttedeJournalposterInternal(DokumentInfoId("456"), null)).isEmpty()
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ufullstendigeSvar")
    fun `ufullstendige svar feiler i stedet for å se ut som manglende tilknytning`(
        beskrivelse: String,
        respons: SafRespons
    ) {
        medSafRespons(respons) { klient ->
            assertThatThrownBy {
                klient.hentTilknyttedeJournalposterInternal(DokumentInfoId("456"), null)
            }.describedAs(beskrivelse).isInstanceOf(IllegalStateException::class.java).hasMessageContaining("SAF")
        }
    }

    @Test
    fun `GraphQL-feil forkastes selv om data inneholder en liste`() {
        val respons = safRespons(listOf(
            SafTilknyttetJournalpost("123", null, listOf(SafTilknyttetDokument("456")))
        )).copy(errors = listOf(
            GraphQLError(
                message = "Ingen tilgang",
                path = listOf("tilknyttedeJournalposter"),
                extensions = GraphQLErrorExtension(ErrorCode.FORBIDDEN, "ExecutionAborted")
            )
        ))

        medSafRespons(respons) { klient ->
            assertThatThrownBy {
                klient.hentTilknyttedeJournalposterInternal(DokumentInfoId("456"), null)
            }.isInstanceOf(IkkeTillattException::class.java)
        }
    }

    @Test
    fun `advarsler gir ikke et tilsynelatende fullstendig resultat`() {
        val respons = safRespons().copy(
            extensions = GraphQLExtensions(listOf(
                GraphQLWarning(null, null, null, "Ufullstendig resultat", null)
            ))
        )

        medSafRespons(respons) { klient ->
            assertThatThrownBy {
                klient.hentTilknyttedeJournalposterInternal(DokumentInfoId("456"), null)
            }.isInstanceOf(IllegalStateException::class.java).hasMessageContaining("advarsler")
        }
    }

    private fun medSafRespons(
        respons: SafRespons,
        test: (SafGraphqlKlient) -> Unit
    ) {
        val server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        val forespørsel = AtomicReference<String>()
        server.createContext("/graphql") { exchange ->
            forespørsel.set(exchange.requestBody.bufferedReader().use { it.readText() })
            val bytes = DefaultJsonMapper.toJson(respons).toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val klient = SafGraphqlKlient(
                RestClient<InputStream>(
                    config = ClientConfig(scope = "saf-test"),
                    tokenProvider = NoTokenTokenProvider(),
                    responseHandler = SafResponseHandler(),
                    prometheus = PrometheusProvider.prometheus
                ),
                URI.create("http://localhost:${server.address.port}/graphql")
            )
            test(klient)
            val request = DefaultJsonMapper.objectMapper().readTree(checkNotNull(forespørsel.get()))
            assertThat(request["variables"]["dokumentInfoId"].asText()).isEqualTo("456")
            assertThat(request["query"].asText()).contains(
                "\$dokumentInfoId: String!",
                "dokumentInfoId: \$dokumentInfoId",
                "tilknytning: GJENBRUK"
            )
        } finally {
            server.stop(0)
        }
    }

    companion object {
        private fun safRespons(journalposter: List<SafTilknyttetJournalpost?> = emptyList()) =
            SafRespons(SafData(journalpost = null, saker = null, tilknyttedeJournalposter = journalposter))

        @JvmStatic
        fun ufullstendigeSvar(): List<Arguments> {
            return listOf(
                Arguments.of("mangler data", SafRespons(data = null)),
                Arguments.of("mangler resultat", SafRespons(SafData(journalpost = null, saker = null))),
                Arguments.of("journalpost er null", safRespons(listOf(null)))
            )
        }
    }
}
