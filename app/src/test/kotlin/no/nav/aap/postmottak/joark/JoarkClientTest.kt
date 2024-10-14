package no.nav.aap.postmottak.joark

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.WithFakes
import no.nav.aap.postmottak.klient.joark.Ident
import no.nav.aap.postmottak.klient.joark.JoarkClient
import no.nav.aap.postmottak.klient.joark.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.junit.jupiter.api.Test


class JoarkClientTest : WithFakes {


    @Test
    fun `før journalpost på fagsak`() {
        val joarkClient = JoarkClient()

        val journalpost: Journalpost.MedIdent = mockk(relaxed = true)
        every { journalpost.personident } returns Ident.Personident("123")
        every { journalpost.journalpostId } returns JournalpostId(1)

        joarkClient.førJournalpostPåFagsak(journalpost, "213412")
    }

    @Test
    fun `før journalpost på generell sak`() {
        val joarkClient = JoarkClient()

        val journalpost: Journalpost.MedIdent = mockk(relaxed = true)
        every { journalpost.personident } returns Ident.Personident("123")
        every { journalpost.journalpostId } returns JournalpostId(1)

        joarkClient.førJournalpostPåGenerellSak(journalpost)
    }

    @Test
    fun `ferdigstillJournalpost happy path`() {
        val joarkClient = JoarkClient()

        val journalpost: Journalpost.MedIdent = mockk(relaxed = true)
        every { journalpost.personident } returns Ident.Personident("123")
        every { journalpost.journalpostId } returns JournalpostId(1)

        joarkClient.ferdigstillJournalpostMaskinelt(journalpost)
    }
}