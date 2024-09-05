package no.nav.aap.behandlingsflyt.joark

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.WithFakes
import no.nav.aap.behandlingsflyt.saf.Ident
import no.nav.aap.behandlingsflyt.saf.Journalpost
import no.nav.aap.behandlingsflyt.test.Fakes
import org.junit.jupiter.api.Test

class JoarkClientTest : WithFakes() {


    @Test
    fun `oppdaterJournalpost happy path`() {
        val joarkClient = JoarkClient()

        val journalpost: Journalpost.MedIdent = mockk(relaxed = true)
        every { journalpost.personident } returns Ident.Personident("123")
        every { journalpost.journalpostId } returns 1

        joarkClient.oppdaterJournalpost(journalpost, "213412")
    }

    @Test
    fun `ferdigstillJournalpost happy path`() {
        val joarkClient = JoarkClient()

        val journalpost: Journalpost.MedIdent = mockk(relaxed = true)
        every { journalpost.personident } returns Ident.Personident("123")
        every { journalpost.journalpostId } returns 1

        joarkClient.ferdigstillJournalpost(journalpost)
    }
}