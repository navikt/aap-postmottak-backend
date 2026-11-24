package no.nav.aap.postmottak.klient.dokarkiv

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.DokarkivGateway
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.JournalpostSak
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.Sakstype
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.postmottak.test.fakes.hoveddokument
import no.nav.aap.postmottak.test.fakes.vedlegg
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.URI
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicReference

@Fakes
class DokarkivGatewayKlientTest {
    private val kilde = TestJournalposter.leggTil {
        papirsøknad()
        status = Journalstatus.JOURNALFOERT
        mottattTid = LocalDateTime.of(2025, 6, 11, 12, 30)
        fagsak = JournalpostSak(sakstype = Sakstype.GENERELL_SAK, fagsaksystem = null)
        dokumenter = listOf(hoveddokument("NAV 11-13.05", "Søknad"), vedlegg("Vedlegg"))
    }
    private val journalpost = kilde.tilJournalpost()

    @Test
    fun `sender alle dokumentene til Kelvin-saken og returnerer ny journalpost-ID`() {
        medDokarkivRespons(KnyttTilAnnenSakResponse(0L)) { klient ->
            assertThat(klient.knyttTilAnnenSak(journalpost, "SAK123", "4491"))
                .isEqualTo(JournalpostId(0L))
        }
    }

    @Test
    fun `manglende ny journalpost-ID gir feil`() {
        val restClient = mockk<RestClient<InputStream>>()
        every {
            restClient.put<KnyttTilAnnenSakRequest, KnyttTilAnnenSakResponse>(any(), any(), any())
        } returns KnyttTilAnnenSakResponse(null)

        assertThatThrownBy {
            DokarkivGatewayKlient(restClient).knyttTilAnnenSak(journalpost, "SAK123", "4491")
        }.isInstanceOf(IllegalStateException::class.java).hasMessageContaining("nyJournalpostId")
    }

    @Test
    fun `registrert klient knytter dokumentene til ny journalpost som kan finnes i SAF`() {
        val gatewayProvider = defaultGatewayProvider()
        val nyJournalpostId = gatewayProvider.provide<DokarkivGateway>()
            .knyttTilAnnenSak(journalpost, "SAK123", "4491")

        assertThat(nyJournalpostId).isNotEqualTo(journalpost.journalpostId)
        val tilknyttetJournalpost = gatewayProvider.provide<JournalpostGateway>()
            .hentTilknyttedeJournalposter(journalpost.dokumenter.first().dokumentInfoId)
            .single { it.journalpostId == nyJournalpostId.toString() }
        assertThat(tilknyttetJournalpost.sak?.fagsakId).isEqualTo("SAK123")
        assertThat(tilknyttetJournalpost.sak?.fagsaksystem).isEqualTo("KELVIN")
        assertThat(tilknyttetJournalpost.sak?.sakstype).isEqualTo(Sakstype.FAGSAK)
        assertThat(tilknyttetJournalpost.dokumenter.map { it.dokumentInfoId })
            .containsExactlyElementsOf(journalpost.dokumenter.map { it.dokumentInfoId.toString() })
        assertThat(TestJournalposter.hentJournalpost(nyJournalpostId.referanse)?.mottattTid)
            .isEqualTo(kilde.mottattTid)
        assertThat(TestJournalposter.hentJournalpost(kilde.journalpostId)).isEqualTo(kilde)
    }

    @Test
    fun `temaendring sender kun tema i journalpostoppdateringen`() {
        val restClient = mockk<RestClient<InputStream>>(relaxed = true)
        val klient = DokarkivGatewayKlient(restClient)

        klient.endreTema(JournalpostId(123L), "BAR", null)

        verify(exactly = 1) {
            restClient.put<EndreTemaRequest, Unit>(withArg {
                assertThat(it.path).isEqualTo("/rest/journalpostapi/v1/journalpost/123")
            }, withArg {
                assertThat(DefaultJsonMapper.fromJson<Map<String, String>>(DefaultJsonMapper.toJson(it.body())))
                    .isEqualTo(mapOf("tema" to "BAR"))
            }, any())
        }
    }

    @Test
    fun `oppretter journalpost for person`() {
        val restClient = mockk<RestClient<InputStream>>(relaxed = true)
        val klient = DokarkivGatewayKlient(restClient)
        val ident = Ident("12345678910")
        val eksternReferanseId = UUID.randomUUID()
        val pdf = "%PDF-1.4\n".toByteArray()

        every {
            restClient.post<OpprettJournalpostRequest, OpprettJournalpostResponse>(any(), any(), any())
        } returns OpprettJournalpostResponse("467011765")

        val journalpostId = klient.opprettJournalpostForPerson(
            ident = ident,
            pdf = pdf,
            filNavn = "filnavn",
            eksternReferanseId = eksternReferanseId
        )

        assertThat(journalpostId).isEqualTo(JournalpostId(467011765))

        verify(exactly = 1) {
            restClient.post<OpprettJournalpostRequest, OpprettJournalpostResponse>(
                withArg {
                    assertThat(it.path).isEqualTo("/rest/journalpostapi/v1/journalpost")
                    assertThat(it.query).contains("forsoekFerdigstilling=false")
                },
                withArg { request ->
                    val body = request.body() as OpprettJournalpostRequest
                    assertThat(body.tema).isEqualTo("AAP")
                    assertThat(body.bruker.id).isEqualTo(ident.identifikator)
                    assertThat(body.kanal).isEqualTo("INNSENDT_NAV_ANSATT")
                    assertThat(body.eksternReferanseId).isEqualTo(eksternReferanseId.toString())
                    val dokument = body.dokumenter.single()
                    assertThat(dokument.tittel).isEqualTo("filnavn")
                    assertThat(dokument.brevkode).isEqualTo("NAVe 11-13.05")
                    assertThat(Base64.getDecoder().decode(dokument.dokumentvarianter.single().fysiskDokument))
                        .isEqualTo(pdf)
                },
                any()
            )
        }
    }

    private fun medDokarkivRespons(
        respons: KnyttTilAnnenSakResponse,
        test: (DokarkivGatewayKlient) -> Unit,
    ) {
        val server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        val forespørsel = AtomicReference<HttpExchange>()
        val innhold = AtomicReference<String>()
        server.createContext("/rest/journalpostapi/v1/journalpost") { exchange ->
            forespørsel.set(exchange)
            innhold.set(exchange.requestBody.bufferedReader().use { it.readText() })
            exchange.responseHeaders.add("Content-Type", "application/json")
            val bytes = DefaultJsonMapper.objectMapper().writeValueAsBytes(respons)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val klient = DokarkivGatewayKlient(
                RestClient.withDefaultResponseHandler(
                    config = ClientConfig(scope = "dokarkiv-test"),
                    tokenProvider = NoTokenTokenProvider(),
                    prometheus = PrometheusProvider.prometheus,
                ),
                URI.create("http://localhost:${server.address.port}"),
            )
            test(klient)

            val request = requireNotNull(forespørsel.get())
            assertThat(request.requestMethod).isEqualTo("PUT")
            assertThat(request.requestURI.path)
                .isEqualTo("/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}/knyttTilAnnenSak")
            val body = DefaultJsonMapper.objectMapper().readTree(requireNotNull(innhold.get()))
            assertThat(body["bruker"]["id"].asText()).isEqualTo(journalpost.person.aktivIdent().identifikator)
            assertThat(body["bruker"]["idType"].asText()).isEqualTo("FNR")
            assertThat(body["dokumenter"].map { it.longValue() }).containsExactly(1L, 2L)
            assertThat(body["fagsakId"].asText()).isEqualTo("SAK123")
            assertThat(body["fagsaksystem"].asText()).isEqualTo("KELVIN")
            assertThat(body["journalfoerendeEnhet"].asText()).isEqualTo("4491")
            assertThat(body["sakstype"].asText()).isEqualTo("FAGSAK")
            assertThat(body["tema"].asText()).isEqualTo("AAP")
        } finally {
            server.stop(0)
        }
    }
}