package no.nav.aap.postmottak.joark

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.WithFakes
import no.nav.aap.postmottak.klient.joark.JoarkClient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.sakogbehandling.journalpost.Person
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.junit.jupiter.api.Test
import java.util.*


class JoarkClientTest : WithFakes {


    @Test
    fun `før journalpost på fagsak`() {
        val joarkClient = JoarkClient()

        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.person } returns Person(
            123,
            identifikator = UUID.randomUUID(),
            identer = listOf(Ident("12345678"))
        )
        every { journalpost.journalpostId } returns JournalpostId(1)

        joarkClient.førJournalpostPåFagsak(journalpost, "213412")
    }

    @Test
    fun `før journalpost på generell sak`() {
        val joarkClient = JoarkClient()

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
        val joarkClient = JoarkClient()

        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.person } returns Person(
            123,
            identifikator = UUID.randomUUID(),
            identer = listOf(Ident("12345678"))
        )
        every { journalpost.journalpostId } returns JournalpostId(1)

        joarkClient.ferdigstillJournalpostMaskinelt(journalpost)
    }
}