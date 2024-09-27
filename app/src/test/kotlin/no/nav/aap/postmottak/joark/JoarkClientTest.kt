package no.nav.aap.postmottak.joark

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.WithFakes
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Ident
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

@Ignore
class JoarkClientTest : WithFakes {


    @Test
    fun `oppdaterJournalpost happy path`() {
        val joarkClient = JoarkClient()

        val journalpost: Journalpost.MedIdent = mockk(relaxed = true)
        every { journalpost.personident } returns Ident.Personident("123")
        every { journalpost.journalpostId } returns JournalpostId(1)

        joarkClient.oppdaterJournalpost(journalpost, "213412")
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