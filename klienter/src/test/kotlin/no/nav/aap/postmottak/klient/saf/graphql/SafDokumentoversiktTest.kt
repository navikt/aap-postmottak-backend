package no.nav.aap.postmottak.klient.saf.graphql

import com.sun.net.httpserver.HttpServer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.SafOversiktDokument
import no.nav.aap.postmottak.gateway.SafDokumentoversikt
import no.nav.aap.postmottak.gateway.SafJournalpostOversikt
import no.nav.aap.postmottak.gateway.SafSideInfo
import no.nav.aap.postmottak.gateway.SafVariantformat
import no.nav.aap.postmottak.gateway.SafVisningsvariant
import no.nav.aap.postmottak.klient.graphql.ErrorCode
import no.nav.aap.postmottak.klient.graphql.GraphQLError
import no.nav.aap.postmottak.klient.graphql.GraphQLErrorExtension
import no.nav.aap.postmottak.klient.graphql.GraphQLExtensions
import no.nav.aap.postmottak.klient.graphql.GraphQLWarning
import no.nav.aap.postmottak.test.FakeServers
import no.nav.aap.postmottak.test.modell.TestPerson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.URI
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference

class SafDokumentoversiktTest {
    private val fnr = TestPerson().aktivIdent().identifikator
    private val token = OidcToken(FakeServers.saksbehandlerToken("Journalpostleser"))
    private val oversikt = SafDokumentoversikt(
        listOf(SafJournalpostOversikt(
            "123", "Ettersendelse", LocalDateTime.of(2026, 11, 27, 12, 0), "AAP", null,
            listOf(SafOversiktDokument(
                "456", "Legeerklæring", null, listOf(SafVisningsvariant(SafVariantformat.ARKIV, true, null))
            ))
        )),
        SafSideInfo("neste-side", true)
    )
    private val respons = SafRespons(SafData(null, null, dokumentoversiktBruker = oversikt))

    @BeforeEach
    fun setUp() {
        PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    @Test
    fun `henter en side med AAP-journalposter for personen med alle journalstatuser`() {
        medSafRespons(respons) { klient ->
            assertThat(klient.hentAapJournalposterInternal(fnr, 10, "forrige-side", token)).isEqualTo(oversikt)
        }
    }

    @Test
    fun `delvise data med tilgangsfeil gir ingen journalpostoversikt`() {
        val feil = GraphQLError(
            "Sensitiv feilmelding", listOf("dokumentoversiktBruker"),
            GraphQLErrorExtension(ErrorCode.FORBIDDEN, "ExecutionAborted")
        )
        medSafRespons(respons.copy(errors = listOf(feil))) { klient ->
            assertThatThrownBy { klient.hentAapJournalposterInternal(fnr, 10, "forrige-side", token) }
                .isInstanceOf(IkkeTillattException::class.java)
                .hasMessageNotContaining("Sensitiv feilmelding")
        }
    }

    @Test
    fun `advarsler manglende oversikt og manglende sluttpeker feiler eksplisitt`() {
        val advarsler = GraphQLExtensions(listOf(GraphQLWarning(null, null, null, "Ufullstendig resultat", null)))
        val svar = listOf(
            respons.copy(extensions = advarsler),
            SafRespons(SafData(null, null)),
            respons.copy(data = SafData(null, null, dokumentoversiktBruker = oversikt.copy(
                sideInfo = SafSideInfo(null, true)
            )))
        )
        svar.forEach { ufullstendig ->
            medSafRespons(ufullstendig) { klient ->
                assertThatThrownBy { klient.hentAapJournalposterInternal(fnr, 10, "forrige-side", token) }
                    .isInstanceOf(IllegalStateException::class.java)
                    .hasMessageContaining("SAF")
            }
        }
    }

    private fun medSafRespons(svar: SafRespons, test: (SafGraphqlKlient) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        val forespørsel = AtomicReference<String>()
        server.createContext("/graphql") { exchange ->
            forespørsel.set(exchange.requestBody.bufferedReader().use { it.readText() })
            val bytes = DefaultJsonMapper.toJson(svar).toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            test(SafGraphqlKlient(
                RestClient<InputStream>(
                    config = ClientConfig(scope = "saf-test"),
                    tokenProvider = NoTokenTokenProvider(),
                    responseHandler = SafResponseHandler(),
                    prometheus = PrometheusProvider.prometheus
                ),
                URI.create("http://localhost:${server.address.port}/graphql")
            ))
            val request = DefaultJsonMapper.objectMapper().readTree(checkNotNull(forespørsel.get()))
            assertThat(request["variables"]["fnr"].asText()).isEqualTo(fnr)
            assertThat(request["variables"]["foerste"].intValue()).isEqualTo(10)
            assertThat(request["variables"]["etter"].asText()).isEqualTo("forrige-side")
            assertThat(request["query"].asText()).contains(
                "brukerId: {id: \$fnr, type: FNR}", "tema: [AAP]", "journalstatuser: []",
                "foerste: \$foerste", "etter: \$etter", "skjerming", "saksbehandlerHarTilgang"
            ).doesNotContain(fnr)
        } finally {
            server.stop(0)
        }
    }
}
