package no.nav.aap.postmottak.joark

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tilJournalpost
import no.nav.aap.postmottak.gateway.AvsenderMottakerDto
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.JournalføringService
import no.nav.aap.postmottak.gateway.OppdaterJournalpostRequest
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.ereg.EREGKlient
import no.nav.aap.postmottak.klient.saf.graphql.SafGraphqlClientCredentialsClient
import no.nav.aap.postmottak.test.FakeUnleash
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakeGatewayProvider
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.postmottak.test.modell.TestPersoner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.util.*

@Fakes
class JoarkClientTest {

    private val gatewayProvider = fakeGatewayProvider {
        register<EREGKlient>()
        register<SafGraphqlClientCredentialsClient>()
    }

    @BeforeEach
    fun setup() {
        PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    @Test
    fun `før journalpost på fagsak`() {
        val joarkClient = JournalføringService(gatewayProvider)

        val testPerson = TestPersoner.leggTil {}
        val journalpostId = TestJournalposter.leggTil { person = testPerson }.journalpostId()

        joarkClient.førJournalpostPåFagsak(
            journalpostId,
            testPerson.aktivIdent(),
            "213412",
            tittel = null,
            avsenderMottaker = null,
            dokumenter = null,
            endretAv = null,
        )
    }

    @Test
    fun `før journalpost på generell sak`() {
        val joarkClient = JournalføringService(gatewayProvider)
        val testPerson = TestPersoner.leggTil {}
        val journalpostId = TestJournalposter.leggTil { person = testPerson }.journalpostId()
        val person = Person(
            id = 1,
            identifikator = UUID.randomUUID(),
            identer = listOf(testPerson.aktivIdent())
        )
        val journalpost = SafGraphqlClientCredentialsClient().hentJournalpost(journalpostId).tilJournalpost(person)

        joarkClient.førJournalpostPåGenerellSak(
            journalpost,
            tittel = null,
            avsenderMottaker = null,
            dokumenter = null,
            endretAv = null
        )
    }

    @Test
    fun `ferdigstillJournalpost happy path`() {
        val joarkClient = JournalføringService(gatewayProvider)
        val testPerson = TestPersoner.leggTil {}
        val journalpostId = TestJournalposter.leggTil { person = testPerson }.journalpostId()

        joarkClient.ferdigstillJournalpostMaskinelt(journalpostId, null)
    }

    @Test
    fun `avsenderMottaker blir satt til samme som bruker dersom den mangler`() {

        val restClient = mockk<RestClient<InputStream>>(relaxed = true)
        val joarkClient =
            JournalføringService.konstruer(
                restClient,
                SafGraphqlClientCredentialsClient(),
                enhetsregisteretGateway = EREGKlient(),
                unleashGateway = FakeUnleash
            )

        val testPerson = TestPersoner.leggTil {}
        val journalpostId = TestJournalposter.leggTil {
            avsenderMottaker = null
            person = testPerson
        }.journalpostId()

        val safJournalpost =
            SafGraphqlClientCredentialsClient().hentJournalpost(journalpostId)

        assertThat(safJournalpost.avsenderMottaker).isNull()

        joarkClient.førJournalpostPåFagsak(
            journalpostId,
            testPerson.aktivIdent(),
            "2344",
            tittel = null,
            avsenderMottaker = null,
            dokumenter = null,
            endretAv = null,
        )

        verify {
            restClient.put<OppdaterJournalpostRequest, Any>(any(), withArg { request ->
                val avsenderMottaker = (request.body() as OppdaterJournalpostRequest).avsenderMottaker
                assertThat(avsenderMottaker?.id).isEqualTo(testPerson.aktivIdent().identifikator)
                assertThat(avsenderMottaker?.idType).isEqualTo(AvsenderMottakerDto.IdType.FNR)
                assertThat(avsenderMottaker?.navn).isEqualTo(null)
            }, any())
        }
    }

    @Test
    fun `avsenderMottaker for utenlandsk organisasjon får type UTL_ORG`() {
        val restClient = mockk<RestClient<InputStream>>(relaxed = true)
        val joarkClient =
            JournalføringService.konstruer(
                restClient,
                SafGraphqlClientCredentialsClient(),
                enhetsregisteretGateway = EREGKlient(),
                unleashGateway = FakeUnleash
            )

        val journalpostId = TestJournalposter.leggTil {
            medUtenlandskOrgnr()
        }.journalpostId()

        val safJournalpost =
            SafGraphqlClientCredentialsClient().hentJournalpost(journalpostId)

        assertThat(safJournalpost.avsenderMottaker).isNull()
        assertThat(safJournalpost.bruker?.type).isEqualTo(BrukerIdType.ORGNR)

        joarkClient.førJournalpostPåFagsak(
            journalpostId,
            TestPersoner.leggTil {}.aktivIdent(),
            "2344",
            tittel = null,
            avsenderMottaker = AvsenderMottakerDto("999999999", AvsenderMottakerDto.IdType.UTL_ORG, "Peppas farm"),
            dokumenter = null,
            endretAv = null,
        )

        verify {
            restClient.put<OppdaterJournalpostRequest, Any>(any(), withArg { request ->
                val avsenderMottaker = (request.body() as OppdaterJournalpostRequest).avsenderMottaker
                assertThat(avsenderMottaker?.id).isEqualTo("999999999")
                assertThat(avsenderMottaker?.idType).isEqualTo(AvsenderMottakerDto.IdType.UTL_ORG)
            }, any())
        }
    }
}