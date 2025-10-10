package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.aap.FakeUnleash
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.TemaVurdering
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.FunnetAvklaringsbehov
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.klient.gosysoppgave.GosysOppgaveKlient
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class AvklarTemaStegTest {

    val avklarTemaRepository: AvklarTemaRepository = mockk(relaxed = true)
    val journalpostRepo: JournalpostRepository = mockk()
    val gosysOppgaveKlient: GosysOppgaveKlient = mockk(relaxed = true)
    val saksnummerRepository: SaksnummerRepository = mockk(relaxed = true)

    val avklarTemaSteg =
        AvklarTemaSteg(
            journalpostRepository = journalpostRepo,
            avklarTemaRepository = avklarTemaRepository,
            gosysOppgaveGateway = gosysOppgaveKlient,
            saksnummerRepository = saksnummerRepository,
            unleashGateway = FakeUnleash
        )


    val journalpost: Journalpost = mockk(relaxed = true)
    val behandlingId = BehandlingId(10)
    val kontekst = FlytKontekstMedPerioder(behandlingId = behandlingId, TypeBehandling.DokumentHåndtering)


    @BeforeEach
    fun before() {
        every { journalpostRepo.hentHvisEksisterer(behandlingId) } returns journalpost
    }

    @AfterEach
    fun after() {
        clearAllMocks()
    }


    @Test
    fun `når automatisk saksbehandling er mulig skal ingen avklaringsbehov bli opprettet`() {
        every { journalpost.erDigitalSøknad() } returns true
        every { journalpost.tema } returns "AAP"
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns null

        val actual = avklarTemaSteg.utfør(kontekst)

        assertEquals(Fullført::class.simpleName, actual::class.simpleName)

    }

    @Test
    fun `naar vi ikke kan behandle automatisk og manuell avklaring er avklart med 'skal til AAP' forventer vi at steget ikke returnerer avklaringsbehov`() {
        every { journalpost.erDigitalSøknad() } returns false
        every { journalpost.tema } returns "AAP"
        every { avklarTemaRepository.hentTemaAvklaring(any())?.skalTilAap } returns true

        val actual = avklarTemaSteg.utfør(kontekst)

        assertEquals(Fullført::class.simpleName, actual::class.simpleName)
    }

    @Test
    fun `når vi ikke kan behandle automatisk og manuell avklaring mangler forventer vi avklaringsbehov AVKLAR_TEMA`() {
        every { journalpost.erDigitalSøknad() } returns false
        every { journalpost.tema } returns "AAP"
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns null

        val actual = avklarTemaSteg.utfør(kontekst)

        assertEquals(actual::class.simpleName, FantAvklaringsbehov::class.simpleName)
        val funnetAvklaringsbehov = actual.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).contains(Definisjon.AVKLAR_TEMA)
    }

    @Test
    fun `når tema har blitt endret fortsetter vi til neste steg`() {
        every { journalpost.tema } returns "ANNET"
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(false, Tema.UKJENT)
        every { gosysOppgaveKlient.finnOppgaverForJournalpost(journalpost.journalpostId, tema = "AAP") } returns listOf(
            1,
            2
        )

        val actual = avklarTemaSteg.utfør(kontekst)

//        verify(exactly = 1) { gosysOppgaveKlient.ferdigstillOppgave(1) }
//        verify(exactly = 1) { gosysOppgaveKlient.ferdigstillOppgave(2) }
        assertThat(actual).isEqualTo(Fullført)
        assertEquals(Fullført::class.simpleName, actual::class.simpleName)
    }

    @Test
    fun `når tema har blitt endret, uten temaavklaring, blir steget fullført`() {
        every { journalpost.tema } returns "ANNET"
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns null
        every {
            gosysOppgaveKlient.finnOppgaverForJournalpost(
                journalpost.journalpostId,
                tema = "AAP"
            )
        } returns emptyList()

        val actual = avklarTemaSteg.utfør(kontekst)

        assertEquals(Fullført::class.simpleName, actual::class.simpleName)
    }

}
