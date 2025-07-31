package no.nav.aap.fordeler.arena.jobber

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.aap.fordeler.arena.ArenaOpprettOppgaveRespons
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.klient.arena.ArenaKlient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SendSøknadTilArenaJobbUtførerTest {
    val flytJobbRepositoryMock = mockk<FlytJobbRepository>(relaxed = true)
    val arenaKlientMock = mockk<ArenaKlient>(relaxed = true)
    val journalpostService = mockk<JournalpostService>(relaxed = true) {
        every { hentJournalpost(any()) } returns mockk(relaxed = true) {
            every { status } returns Journalstatus.MOTTATT
        }
    }
    val sendSøknadTilArenaJobb = SendSøknadTilArenaJobbUtfører(flytJobbRepositoryMock, arenaKlientMock, journalpostService)


    @Test
    fun `Skal opprette jobb for manuell oppgave dersom det finnes aktiv sak`() {
        every {arenaKlientMock.harAktivSak(any())} returns true
        
        val journalpostId = JournalpostId(1)
        
        val jobbKontekst =   ArenaVideresenderKontekst(
            journalpostId = journalpostId,
            innkommendeJournalpostId = 1L,
            ident = Ident("123"),
            hoveddokumenttittel = "Hoveddokument",
            vedleggstitler = listOf("Vedlegg"),
            navEnhet = "4491"
        )
        
        val jobbInput = JobbInput(SendSøknadTilArenaJobbUtfører).medArenaVideresenderKontekst(
            jobbKontekst
        )
        sendSøknadTilArenaJobb.utfør(jobbInput)
        
        verify(exactly = 1) {flytJobbRepositoryMock.leggTil(withArg{
            assertThat(it.type()).isEqualTo(ManuellJournalføringJobbUtfører.type())
            assertThat(it.getArenaVideresenderKontekst()).isEqualTo(jobbKontekst)
        })}
    }
    
    @Test
    fun `Skal opprette jobb for automatisk journalføring dersom det ikke finnes en sak i arena fra før`() {
        every {arenaKlientMock.harAktivSak(any())} returns false
        
        val journalpostId = JournalpostId(1)
        
        val jobbKontekst =   ArenaVideresenderKontekst(
            journalpostId = journalpostId,
            innkommendeJournalpostId = 1L,
            ident = Ident("123"),
            hoveddokumenttittel = "Hoveddokument",
            vedleggstitler = listOf("Vedlegg"),
            navEnhet = "4491"
        )
        
        val jobbInput = JobbInput(SendSøknadTilArenaJobbUtfører).medArenaVideresenderKontekst(
            jobbKontekst
        )

        every {arenaKlientMock.opprettArenaOppgave(any())} returns ArenaOpprettOppgaveRespons("", "sakId")

        sendSøknadTilArenaJobb.utfør(jobbInput)
        
        verify(exactly = 1) {flytJobbRepositoryMock.leggTil(withArg{
            assertThat(it.type()).isEqualTo(AutomatiskJournalføringJobbUtfører.type())
            assertThat(it.getAutomatiskJournalføringKontekst()).isEqualTo(AutomatiskJournalføringKontekst(
                journalpostId = journalpostId,
                innkommendeJournalpostId = 1L,
                ident = jobbKontekst.ident,
                saksnummer = "sakId"
            ))
        })}
            
    }

    @Test
    fun `journalpost skal sendes til manuell journalføring når Sendsøknadjobb har feilet 3 ganger`() {
        every {arenaKlientMock.harAktivSak(any())} returns false

        val journalpostId = JournalpostId(1)

        val jobbKontekst =   ArenaVideresenderKontekst(
            journalpostId = journalpostId,
            innkommendeJournalpostId = 1L,
            ident = Ident("123"),
            hoveddokumenttittel = "Hoveddokument",
            vedleggstitler = listOf("Vedlegg"),
            navEnhet = "4491"
        )

        val jobbInput = spyk(JobbInput(SendSøknadTilArenaJobbUtfører).medArenaVideresenderKontekst(
            jobbKontekst
        ))

        every { jobbInput.antallRetriesForsøkt() } returns 3

        sendSøknadTilArenaJobb.utfør(jobbInput)

        verify(exactly = 1) {flytJobbRepositoryMock.leggTil(withArg{
            assertThat(it.type()).isEqualTo(ManuellJournalføringJobbUtfører.type())
            assertThat(it.getArenaVideresenderKontekst()).isEqualTo(jobbKontekst)
        })}
    }
}