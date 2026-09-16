package no.nav.aap.fordeler.arena.jobber

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import org.junit.jupiter.api.Test

class ManuellJournalføringJobbTest {
    private val gosysMock = mockk<GosysOppgaveGateway>(relaxed = true)
    private val journalpostServiceMock = mockk<JournalpostService>()

    private val manuellJournalføringJobb = ManuellJournalføringJobbUtfører(
        gosysMock,
        journalpostServiceMock,
    )

    @Test
    fun `Skal opprette fordelingsoppgave hvis oppretting av journalføringsoppgave har feilet 3 ganger`() {
        val journalpost = TestJournalposter.leggTil {
            status = Journalstatus.MOTTATT
        }.tilJournalpost()
        every { journalpostServiceMock.hentJournalpost(any()) } returns journalpost
        every {gosysMock.finnOppgaverForJournalpost(any(), any(), any())} returns emptyList()
        
        val kontekst =   ArenaVideresenderKontekst(
            journalpostId = JournalpostId(1),
            innkommendeJournalpostId = 1L,
            ident = Ident("123"),
            hoveddokumenttittel = "Hoveddokument",
            vedleggstitler = listOf("Vedlegg"),
            navEnhet = "4491"
        )
        val jobbinput = spyk(JobbInput(ManuellJournalføringJobbUtfører).medArenaVideresenderKontekst(kontekst))
        every { jobbinput.antallRetriesForsøkt() } returns 3
        manuellJournalføringJobb.utfør(jobbinput)
        
        verify(exactly = 1) {gosysMock.opprettFordelingsOppgaveHvisIkkeEksisterer(any(), any(), kontekst.ident, any())}
    }
}