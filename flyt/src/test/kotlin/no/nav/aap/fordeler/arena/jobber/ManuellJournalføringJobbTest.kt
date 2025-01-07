package no.nav.aap.fordeler.arena.jobber

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.fakes.WithFakes
import org.junit.jupiter.api.Test

class ManuellJournalføringJobbTest: WithFakes {
    val gosysMock = mockk<GosysOppgaveGateway>(relaxed = true)
    val journalpostServiceMock = mockk<JournalpostGateway>(relaxed = true)
    val journalpostMock = mockk<SafJournalpost>(relaxed = true)

    val manuellJournalføringJobb = ManuellJournalføringJobbUtfører(
        gosysMock,
        journalpostServiceMock
    )
    
    @Test
    fun `Skal ikke opprette oppgave hvis journalposten allerede er ferdigstilt`() {
        every { journalpostMock.journalstatus} returns Journalstatus.FERDIGSTILT
        every { journalpostServiceMock.hentJournalpost(any()) } returns journalpostMock
        every {gosysMock.finnOppgaverForJournalpost(any())} returns emptyList()

        val kontekst =   ArenaVideresenderKontekst(
            journalpostId = JournalpostId(1),
            ident = Ident("123"),
            hoveddokumenttittel = "Hoveddokument",
            vedleggstitler = listOf("Vedlegg"),
            navEnhet = "4491"
        )
        val jobbinput = JobbInput(ManuellJournalføringJobbUtfører).medArenaVideresenderKontekst(kontekst)
        manuellJournalføringJobb.utfør(jobbinput)
        
        verify(exactly = 0) {gosysMock.opprettJournalføringsOppgave(any(), any(), any(), any())}
        verify(exactly = 0) {gosysMock.opprettFordelingsOppgave(any(), any(), any())}
    }
    
    @Test
    fun `Skal ikke opprette oppgave dersom det finnes åpen oppgave`() {
        every { journalpostMock.journalstatus} returns Journalstatus.MOTTATT
        every { journalpostServiceMock.hentJournalpost(any()) } returns journalpostMock
        every {gosysMock.finnOppgaverForJournalpost(any(), any())} returns listOf(1)

        val kontekst =   ArenaVideresenderKontekst(
            journalpostId = JournalpostId(1),
            ident = Ident("123"),
            hoveddokumenttittel = "Hoveddokument",
            vedleggstitler = listOf("Vedlegg"),
            navEnhet = "4491"
        )
        val jobbinput = JobbInput(ManuellJournalføringJobbUtfører).medArenaVideresenderKontekst(kontekst)
        manuellJournalføringJobb.utfør(jobbinput)
        
        verify(exactly = 0) {gosysMock.opprettJournalføringsOppgave(any(), any(), any(), any())}
        verify(exactly = 0) {gosysMock.opprettFordelingsOppgave(any(), any(), any())}
    }
    
    @Test
    fun `Skal opprette fordelingsoppgave hvis oppretting av journalføringsoppgave har feilet 3 ganger`() {
        every { journalpostMock.journalstatus} returns Journalstatus.MOTTATT
        every { journalpostServiceMock.hentJournalpost(any()) } returns journalpostMock
        every {gosysMock.finnOppgaverForJournalpost(any(), any())} returns emptyList()
        
        val kontekst =   ArenaVideresenderKontekst(
            journalpostId = JournalpostId(1),
            ident = Ident("123"),
            hoveddokumenttittel = "Hoveddokument",
            vedleggstitler = listOf("Vedlegg"),
            navEnhet = "4491"
        )
        val jobbinput = spyk(JobbInput(ManuellJournalføringJobbUtfører).medArenaVideresenderKontekst(kontekst))
        every { jobbinput.antallRetriesForsøkt() } returns 3
        manuellJournalføringJobb.utfør(jobbinput)
        
        verify(exactly = 1) {gosysMock.opprettFordelingsOppgave(any(), any(), any())}
    }
}