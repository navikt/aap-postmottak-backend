package no.nav.aap.fordeler.arena

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.fordeler.Enhetsutreder
import no.nav.aap.fordeler.arena.jobber.ArenaVideresenderKontekst
import no.nav.aap.fordeler.arena.jobber.AutomatiskJournalføringJobbUtfører
import no.nav.aap.fordeler.arena.jobber.AutomatiskJournalføringKontekst
import no.nav.aap.fordeler.arena.jobber.ManuellJournalføringJobbUtfører
import no.nav.aap.fordeler.arena.jobber.SendSøknadTilArenaJobbUtfører
import no.nav.aap.fordeler.arena.jobber.getArenaVideresenderKontekst
import no.nav.aap.fordeler.arena.jobber.getAutomatiskJournalføringKontekst
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.JournalføringsGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.JournalpostMedDokumentTitler
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.arena.ArenaKlient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class ArenaVideresenderTest {

    val journalpostService: JournalpostService = mockk()
    val joarkClient: JournalføringsGateway = mockk(relaxed = true)
    val flytJobbRepository: FlytJobbRepository = mockk(relaxed = true)
    val enhetsutreder: Enhetsutreder = mockk()
    val arenaKlient: ArenaKlient = mockk()

    val arenaVideresender = ArenaVideresender(
        journalpostService,
        joarkClient,
        flytJobbRepository,
        enhetsutreder,
        arenaKlient
    )

    @Test
    fun `når journalpost er en legeerklæring, skal journalposten journalføres med tema OPP`() {

        val journalpostId = JournalpostId(1)
        val journalpost: JournalpostMedDokumentTitler = mockk()
        every { journalpost.hoveddokumentbrevkode } returns Brevkoder.LEGEERKLÆRING.kode
        every { journalpost.journalpostId } returns journalpostId

        every { journalpostService.hentjournalpost(journalpostId) } returns journalpost

        arenaVideresender.videresendJournalpostTilArena(journalpostId)

        verify(exactly = 1) { joarkClient.førJournalpostPåGenerellSak(journalpost, "OPP") }
        verify(exactly = 1) { joarkClient.ferdigstillJournalpostMaskinelt(journalpostId) }

    }

    @Test
    fun `når journalpost er en søknad, skal en SendSøknadTilArenaJobb opprettes`() {

        val actualKontekst = ArenaVideresenderKontekst(
            journalpostId = JournalpostId(1),
            ident = Ident("1"),
            navEnhet = "enhet",
            hoveddokumenttittel = "hoveddokumenttittel",
            vedleggstitler =  listOf("vedleggtitler")
        )

        val journalpost: JournalpostMedDokumentTitler = mockk {
            every { hoveddokumentbrevkode } returns Brevkoder.SØKNAD.kode
            every { journalpostId } returns actualKontekst.journalpostId
            every { person } returns mockk { every{aktivIdent()} returns actualKontekst.ident }
            every { getHoveddokumenttittel() } returns actualKontekst.hoveddokumenttittel
            every { getVedleggTitler() } returns actualKontekst.vedleggstitler
        }

        every { journalpostService.hentjournalpost(actualKontekst.journalpostId) } returns journalpost
        every { enhetsutreder.finnNavenhetForJournalpost(journalpost) } returns actualKontekst.navEnhet

        arenaVideresender.videresendJournalpostTilArena(actualKontekst.journalpostId)

        verify { flytJobbRepository.leggTil(withArg {
            assertEquals(it.type(), SendSøknadTilArenaJobbUtfører.type())
            assertEquals(it.getArenaVideresenderKontekst(), actualKontekst)
        }) }

    }

    @Test
    fun `når journalpost er en søknadsettersendelse, skal en AutomatiskJournalføringsjobb opprettes`() {

        val automatiskJournalføringKontekst = AutomatiskJournalføringKontekst(
            journalpostId = JournalpostId(1),
            ident = Ident("1"),
            saksnummer = "saksnummer"
        )

        val journalpost: JournalpostMedDokumentTitler = mockk {
            every { journalpostId } returns automatiskJournalføringKontekst.journalpostId
            every { person } returns Person(1, UUID.randomUUID(), listOf(automatiskJournalføringKontekst.ident))
            every { hoveddokumentbrevkode } returns Brevkoder.STANDARD_ETTERSENDING.kode
        }

        every { journalpostService.hentjournalpost(automatiskJournalføringKontekst.journalpostId) } returns journalpost

        every { arenaKlient.nyesteAktiveSak(automatiskJournalføringKontekst.ident) } returns "saksnummer"

        arenaVideresender.videresendJournalpostTilArena(automatiskJournalføringKontekst.journalpostId)

        verify(exactly = 1) { flytJobbRepository.leggTil(withArg {
            assertEquals(it.type(), AutomatiskJournalføringJobbUtfører.type())
            assertEquals(it.getAutomatiskJournalføringKontekst(), automatiskJournalføringKontekst)
        }) }

    }

    @Test
    fun `når journalposttyper som ikke har særregler skal gå til manuell journalføring`() {

        val actualKontekst = ArenaVideresenderKontekst(
            journalpostId = JournalpostId(1),
            ident = Ident("1"),
            navEnhet = "enhet",
            hoveddokumenttittel = "hoveddokumenttittel",
            vedleggstitler =  listOf("vedleggtitler")
        )

        val journalpost: JournalpostMedDokumentTitler = mockk {
            every { hoveddokumentbrevkode } returns "something else"
            every { journalpostId } returns actualKontekst.journalpostId
            every { person } returns mockk { every{aktivIdent()} returns actualKontekst.ident }
            every { getHoveddokumenttittel() } returns actualKontekst.hoveddokumenttittel
            every { getVedleggTitler() } returns actualKontekst.vedleggstitler
        }

        every { journalpostService.hentjournalpost(actualKontekst.journalpostId) } returns journalpost
        every { enhetsutreder.finnNavenhetForJournalpost(journalpost) } returns actualKontekst.navEnhet

        arenaVideresender.videresendJournalpostTilArena(actualKontekst.journalpostId)

        verify { flytJobbRepository.leggTil(withArg {
            assertEquals(it.type(), ManuellJournalføringJobbUtfører.type())
            assertEquals(it.getArenaVideresenderKontekst(), actualKontekst)
        }) }

    }

}