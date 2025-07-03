package no.nav.aap.fordeler.arena

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.arena.jobber.ArenaVideresenderKontekst
import no.nav.aap.fordeler.arena.jobber.ManuellJournalføringJobbUtfører
import no.nav.aap.fordeler.arena.jobber.OppprettOppgaveIArenaJobbUtfører
import no.nav.aap.fordeler.arena.jobber.SendSøknadTilArenaJobbUtfører
import no.nav.aap.fordeler.arena.jobber.getArenaVideresenderKontekst
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.JournalføringsGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.JournalpostMedDokumentTitler
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class ArenaVideresenderTest {
    val journalpostService: JournalpostService = mockk()
    val joarkClient: JournalføringsGateway = mockk(relaxed = true)
    val flytJobbRepository: FlytJobbRepository = mockk(relaxed = true)
    val innkommendeJournalpostRepository: InnkommendeJournalpostRepository = mockk(relaxed = true)

    val arenaVideresender = ArenaVideresender(
        journalpostService,
        joarkClient,
        flytJobbRepository,
        innkommendeJournalpostRepository,
    )

    @Test
    fun `når journalpost er en legeerklæring, skal journalposten journalføres med tema OPP`() {

        val journalpostId_ = JournalpostId(1)
        val journalpost: JournalpostMedDokumentTitler = mockk<JournalpostMedDokumentTitler> {
            every { hoveddokumentbrevkode } returns Brevkoder.LEGEERKLÆRING.kode
            every { journalpostId } returns journalpostId_
            every { getHoveddokumenttittel() } returns "Hoveddokumenttittel"
            every { getVedleggTitler() } returns listOf("Vedlegg")
            every { person } returns mockk { every { aktivIdent() } returns Ident("1") }
            every { status } returns Journalstatus.MOTTATT

        }
        every { innkommendeJournalpostRepository.hent(journalpostId_) } returns mockk {
            every { enhet } returns "enhet"
        }

        every { journalpostService.hentJournalpostMedDokumentTitler(journalpostId_) } returns journalpost

        arenaVideresender.videresendJournalpostTilArena(
            journalpostId_, innkommendeJournalpostId = 1L,
        )

        verify(exactly = 1) { joarkClient.førJournalpostPåGenerellSak(journalpost, "OPP") }
        verify(exactly = 1) { joarkClient.ferdigstillJournalpostMaskinelt(journalpostId_) }

    }

    @Test
    fun `når journalpost er en søknad, skal en SendSøknadTilArenaJobb opprettes`() {
        val actualKontekst = ArenaVideresenderKontekst(
            journalpostId = JournalpostId(1),
            innkommendeJournalpostId = 1L,
            ident = Ident("1"),
            navEnhet = "enhet",
            hoveddokumenttittel = "hoveddokumenttittel",
            vedleggstitler = listOf("vedleggtitler")
        )

        val journalpost: JournalpostMedDokumentTitler = mockk {
            every { hoveddokumentbrevkode } returns Brevkoder.SØKNAD.kode
            every { journalpostId } returns actualKontekst.journalpostId
            every { person } returns mockk { every { aktivIdent() } returns actualKontekst.ident }
            every { getHoveddokumenttittel() } returns actualKontekst.hoveddokumenttittel
            every { getVedleggTitler() } returns actualKontekst.vedleggstitler
            every { status } returns Journalstatus.MOTTATT
        }

        every { journalpostService.hentJournalpostMedDokumentTitler(actualKontekst.journalpostId) } returns journalpost
        every { innkommendeJournalpostRepository.hent(actualKontekst.journalpostId) } returns mockk {
            every { enhet } returns actualKontekst.navEnhet
        }

        arenaVideresender.videresendJournalpostTilArena(
            actualKontekst.journalpostId, innkommendeJournalpostId = 1L
        )

        verify {
            flytJobbRepository.leggTil(withArg {
                assertEquals(it.type(), SendSøknadTilArenaJobbUtfører.type())
                assertEquals(it.getArenaVideresenderKontekst(), actualKontekst)
            })
        }

    }

    @Test
    fun `når journalpost er en søknadsettersendelse, skal en AutomatiskJournalføringsjobb opprettes`() {

        val arenaVideresenderKontekst = ArenaVideresenderKontekst(
            journalpostId = JournalpostId(1),
            innkommendeJournalpostId = 1L,
            ident = Ident("1"),
            hoveddokumenttittel = "hoveddokumenttittel",
            vedleggstitler = listOf("vedleggtittel"),
            navEnhet = "Utland"
        )

        val journalpost: JournalpostMedDokumentTitler = mockk {
            every { journalpostId } returns arenaVideresenderKontekst.journalpostId
            every { person } returns Person(1, UUID.randomUUID(), listOf(arenaVideresenderKontekst.ident))
            every { hoveddokumentbrevkode } returns Brevkoder.STANDARD_ETTERSENDING.kode
            every { getHoveddokumenttittel() } returns arenaVideresenderKontekst.hoveddokumenttittel
            every { getVedleggTitler() } returns arenaVideresenderKontekst.vedleggstitler
            every { status } returns Journalstatus.MOTTATT
        }

        every { innkommendeJournalpostRepository.hent(arenaVideresenderKontekst.journalpostId) } returns mockk {
            every { enhet } returns arenaVideresenderKontekst.navEnhet
        }

        every { journalpostService.hentJournalpostMedDokumentTitler(arenaVideresenderKontekst.journalpostId) } returns journalpost

        arenaVideresender.videresendJournalpostTilArena(
            arenaVideresenderKontekst.journalpostId,
            innkommendeJournalpostId = 1L,
        )

        verify(exactly = 1) {
            flytJobbRepository.leggTil(withArg {
                assertEquals(it.type(), OppprettOppgaveIArenaJobbUtfører.type())
                assertEquals(it.getArenaVideresenderKontekst(), arenaVideresenderKontekst)
            })
        }

    }

    @Test
    fun `når journalposttyper som ikke har særregler skal gå til manuell journalføring`() {

        val actualKontekst = ArenaVideresenderKontekst(
            journalpostId = JournalpostId(1),
            innkommendeJournalpostId = 1L,

            ident = Ident("1"),
            navEnhet = "enhet",
            hoveddokumenttittel = "hoveddokumenttittel",
            vedleggstitler = listOf("vedleggtitler")
        )

        val journalpost: JournalpostMedDokumentTitler = mockk {
            every { hoveddokumentbrevkode } returns "something else"
            every { journalpostId } returns actualKontekst.journalpostId
            every { person } returns mockk { every { aktivIdent() } returns actualKontekst.ident }
            every { getHoveddokumenttittel() } returns actualKontekst.hoveddokumenttittel
            every { getVedleggTitler() } returns actualKontekst.vedleggstitler
            every { status } returns Journalstatus.MOTTATT
        }

        every { journalpostService.hentJournalpostMedDokumentTitler(actualKontekst.journalpostId) } returns journalpost
        every { innkommendeJournalpostRepository.hent(actualKontekst.journalpostId) } returns mockk {
            every { enhet } returns actualKontekst.navEnhet
        }

        arenaVideresender.videresendJournalpostTilArena(actualKontekst.journalpostId, innkommendeJournalpostId = 1L)

        verify {
            flytJobbRepository.leggTil(withArg {
                assertEquals(it.type(), ManuellJournalføringJobbUtfører.type())
                assertEquals(it.getArenaVideresenderKontekst(), actualKontekst)
            })
        }

    }

    @Test
    fun `Allerede journalførte journalposter skal ikke føre til nye oppgaver i Arena eller Gosys`() {

        val journalpostId_ = JournalpostId(1)
        val journalpost: JournalpostMedDokumentTitler = mockk {
            every { journalpostId } returns journalpostId_
            every { status } returns Journalstatus.JOURNALFOERT
        }

        every { journalpostService.hentJournalpostMedDokumentTitler(journalpostId_) } returns journalpost

        arenaVideresender.videresendJournalpostTilArena(
            journalpostId_, innkommendeJournalpostId = 1L,
        )

        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
    }

}