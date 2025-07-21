package no.nav.aap.postmottak.joark

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.JournalføringsGateway
import no.nav.aap.postmottak.gateway.OppdaterJournalpostRequest
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.joark.JoarkClient
import no.nav.aap.postmottak.klient.pdl.PdlGraphqlKlient
import no.nav.aap.postmottak.klient.saf.graphql.SafGraphqlClientCredentialsClient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.DEFAULT_IDENT
import no.nav.aap.postmottak.test.fakes.UTEN_AVSENDER_MOTTAKER
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.util.*

@Fakes
class JoarkClientTest {

    @BeforeEach
    fun setup() {
        PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        GatewayRegistry.register(SafGraphqlClientCredentialsClient::class)
        GatewayRegistry.register(JoarkClient::class)
        GatewayRegistry.register(PdlGraphqlKlient::class)
    }

    @Test
    fun `før journalpost på fagsak`() {
        val joarkClient = GatewayProvider.provide(JournalføringsGateway::class)

        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.person } returns Person(
            123,
            identifikator = UUID.randomUUID(),
            identer = listOf(Ident("12345678"))
        )
        every { journalpost.journalpostId } returns JournalpostId(1)
        joarkClient.førJournalpostPåFagsak(
            journalpost.journalpostId,
            journalpost.person.aktivIdent(),
            "213412",
            tittel = null,
            avsenderMottaker = null,
            dokumenter = null
        )
    }

    @Test
    fun `før journalpost på generell sak`() {
        val joarkClient = GatewayProvider.provide(JournalføringsGateway::class)

        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.person } returns Person(
            123,
            identifikator = UUID.randomUUID(),
            identer = listOf(Ident("12345678"))
        )
        every { journalpost.journalpostId } returns JournalpostId(1)

        joarkClient.førJournalpostPåGenerellSak(journalpost, tittel = null, avsenderMottaker = null, dokumenter = null)
    }

    @Test
    fun `ferdigstillJournalpost happy path`() {
        val joarkClient = GatewayProvider.provide(JournalføringsGateway::class)

        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.person } returns Person(
            123,
            identifikator = UUID.randomUUID(),
            identer = listOf(Ident("12345678"))
        )
        every { journalpost.journalpostId } returns JournalpostId(1)

        joarkClient.ferdigstillJournalpostMaskinelt(journalpost.journalpostId)
    }

    @Test
    fun `avsenderMottaker blir satt til samme som bruker dersom den mangler`() {

        val restClient = mockk<RestClient<InputStream>>(relaxed = true)
        val joarkClient = JoarkClient.konstruer(restClient, SafGraphqlClientCredentialsClient(), PdlGraphqlKlient())

        val safJournalpost = SafGraphqlClientCredentialsClient().hentJournalpost(UTEN_AVSENDER_MOTTAKER)

        assertThat(safJournalpost.avsenderMottaker).isNull()

        joarkClient.førJournalpostPåFagsak(
            UTEN_AVSENDER_MOTTAKER,
            DEFAULT_IDENT,
            "2344",
            tittel = null,
            avsenderMottaker = null,
            dokumenter = null
        )

        verify {
            restClient.put<OppdaterJournalpostRequest, Any>(any(), withArg { request ->
                val avsenderMottaker = (request.body() as OppdaterJournalpostRequest).avsenderMottaker
                assertThat(avsenderMottaker?.id).isEqualTo(DEFAULT_IDENT.identifikator)
                assertThat(avsenderMottaker?.idType).isEqualTo(BrukerIdType.FNR)
                assertThat(avsenderMottaker?.navn).isEqualTo("Ola Normann")
            }, any())
        }
    }
}