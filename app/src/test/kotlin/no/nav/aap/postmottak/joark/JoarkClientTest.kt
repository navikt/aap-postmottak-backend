package no.nav.aap.postmottak.joark

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.postmottak.klient.joark.JoarkClient
import no.nav.aap.postmottak.klient.joark.OppdaterJournalpostRequest
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.saf.graphql.BrukerIdType
import no.nav.aap.postmottak.saf.graphql.SafGraphqlKlient
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.sakogbehandling.journalpost.Person
import no.nav.aap.postmottak.test.fakes.UTEN_AVSENDER_MOTTAKER
import no.nav.aap.postmottak.test.fakes.WithFakes
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.util.*


class JoarkClientTest : WithFakes {


    @Test
    fun `før journalpost på fagsak`() {
        val joarkClient = JoarkClient.withClientCridentialsTokenProvider()

        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.person } returns Person(
            123,
            identifikator = UUID.randomUUID(),
            identer = listOf(Ident("12345678"))
        )
        every { journalpost.journalpostId } returns JournalpostId(1)

        joarkClient.førJournalpostPåFagsak(journalpost.journalpostId, journalpost.person.aktivIdent(), "213412")
    }

    @Test
    fun `før journalpost på generell sak`() {
        val joarkClient = JoarkClient.withClientCridentialsTokenProvider()

        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.person } returns Person(
            123,
            identifikator = UUID.randomUUID(),
            identer = listOf(Ident("12345678"))
        )
        every { journalpost.journalpostId } returns JournalpostId(1)

        joarkClient.førJournalpostPåGenerellSak(journalpost)
    }

    @Test
    fun `ferdigstillJournalpost happy path`() {
        val joarkClient = JoarkClient.withClientCridentialsTokenProvider()

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
        val joarkClient = JoarkClient(restClient, SafGraphqlKlient.withClientCredentialsRestClient())
        val ident = Ident("213453452")

        val safJournalpost = SafGraphqlKlient.withClientCredentialsRestClient().hentJournalpost(UTEN_AVSENDER_MOTTAKER)

        assertThat(safJournalpost.avsenderMottaker).isNull()

        joarkClient.førJournalpostPåFagsak(UTEN_AVSENDER_MOTTAKER, ident, "2344")

        verify { restClient.put<OppdaterJournalpostRequest, Any>(any(), withArg { request ->
            val avsenderMottaker = (request.body() as OppdaterJournalpostRequest).avsenderMottaker
            assertThat(avsenderMottaker?.id).isEqualTo("213453452")
            assertThat(avsenderMottaker?.type).isEqualTo(BrukerIdType.FNR)
        }, any()) }
    }
}