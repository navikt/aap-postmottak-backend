package no.nav.aap.postmottak.klient.dokarkiv

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.Fakes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.util.*

@Fakes
class DokarkivGatewayKlientTest {
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
}