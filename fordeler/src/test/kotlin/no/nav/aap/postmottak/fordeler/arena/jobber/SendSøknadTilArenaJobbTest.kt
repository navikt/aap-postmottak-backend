package no.nav.aap.postmottak.fordeler.arena.jobber

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.klient.arena.ArenaKlient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.fakes.WithFakes
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SendSøknadTilArenaJobbTest: WithFakes {
    val flytJobbRepositoryMock = mockk<FlytJobbRepository>(relaxed = true)
    val arenaKlientMock = mockk<ArenaKlient>(relaxed = true)
    val sendSøknadTilArenaJobb = SendSøknadTilArenaJobb(flytJobbRepositoryMock, arenaKlientMock)

    @Test 
    fun `Skal opprette jobb for automatisk journalføring dersom det finnes aktiv sak`() {
        every {arenaKlientMock.harAktivSak(any())} returns true
        
        val journalpostId = JournalpostId(1)
        
        val jobbKontekst =   ArenaVideresenderKontekst(
            journalpostId = journalpostId,
            ident = Ident("123"),
            hoveddokumenttittel = "Hoveddokument",
            vedleggstittler = listOf("Vedlegg"),
            navEnhet = "4491"
        )
        
        val jobbInput = JobbInput(SendSøknadTilArenaJobb).medArenaVideresenderKontekst(
            jobbKontekst
        )
        sendSøknadTilArenaJobb.utfør(jobbInput)
        
        verify(exactly = 1) {flytJobbRepositoryMock.leggTil(withArg{
            assertThat(it.type()).isEqualTo(AutomatiskJournalføringsjobb.type())
            val actualKontekst = DefaultJsonMapper.fromJson<ArenaVideresenderKontekst>(it.payload())
            assertThat(actualKontekst).isEqualTo(jobbKontekst)
        })}
    }
    
    @Test
    fun `Skal opprette jobb for manuell oppgave dersom det ikke finnes aktiv sak`() {
        every {arenaKlientMock.harAktivSak(any())} returns false
        
        val journalpostId = JournalpostId(1)
        
        val jobbKontekst =   ArenaVideresenderKontekst(
            journalpostId = journalpostId,
            ident = Ident("123"),
            hoveddokumenttittel = "Hoveddokument",
            vedleggstittler = listOf("Vedlegg"),
            navEnhet = "4491"
        )
        
        val jobbInput = JobbInput(SendSøknadTilArenaJobb).medArenaVideresenderKontekst(
            jobbKontekst
        )
        sendSøknadTilArenaJobb.utfør(jobbInput)
        
        verify(exactly = 1) {flytJobbRepositoryMock.leggTil(withArg{
            assertThat(it.type()).isEqualTo(ManuellJournalføringsoppgavejobb.type())
            val actualKontekst = DefaultJsonMapper.fromJson<ArenaVideresenderKontekst>(it.payload())
            assertThat(actualKontekst).isEqualTo(jobbKontekst)
        })}
            
    }
}