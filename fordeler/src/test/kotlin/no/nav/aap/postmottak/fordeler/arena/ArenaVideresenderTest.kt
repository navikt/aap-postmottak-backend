package no.nav.aap.postmottak.fordeler.arena

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.fordeler.Enhetsutreder
import no.nav.aap.postmottak.fordeler.arena.jobber.ArenaVideresenderKontekst
import no.nav.aap.postmottak.fordeler.arena.jobber.SendSøknadTilArenaJobbUtfører
import no.nav.aap.postmottak.fordeler.arena.jobber.getArenaVideresenderKontekst
import no.nav.aap.postmottak.klient.joark.JoarkClient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.journalpost.JournalpostMedDokumentTitler
import no.nav.aap.verdityper.Brevkoder
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArenaVideresenderTest {

    val journalpostService: JournalpostService = mockk()
    val joarkClient: JoarkClient = mockk(relaxed = true)
    val flytJobbRepository: FlytJobbRepository = mockk(relaxed = true)
    val enhetsutreder: Enhetsutreder = mockk()

    val arenaVideresender = ArenaVideresender(
        journalpostService,
        joarkClient,
        flytJobbRepository,
        enhetsutreder
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

}