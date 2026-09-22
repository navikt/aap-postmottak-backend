package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.TemaVurdering
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.klient.gosysoppgave.GosysOppgaveKlient
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.fakes.InMemoryAvklaringsbehovRepository
import no.nav.aap.postmottak.test.fakes.TestJournalPost
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
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
    val unleashGateway: UnleashGateway = mockk(relaxed = true)

    val avklarTemaSteg =
        AvklarTemaSteg(
            journalpostRepository = journalpostRepo,
            avklarTemaRepository = avklarTemaRepository,
            gosysOppgaveGateway = gosysOppgaveKlient,
            saksnummerRepository = saksnummerRepository,
            avklaringsbehovService = AvklaringsbehovService(InMemoryAvklaringsbehovRepository),
            unleashGateway = unleashGateway,
        )

    val behandlingId = BehandlingId(10)
    val kontekst = FlytKontekst(
        behandlingId = behandlingId,
        behandlingType = TypeBehandling.DokumentHåndtering,
        journalpostId = JournalpostId(1)
    )

    @BeforeEach
    fun before() {
        InMemoryAvklaringsbehovRepository.clearMemory()
    }

    @AfterEach
    fun after() {
        clearAllMocks()
    }


    @Test
    fun `når automatisk saksbehandling er mulig skal ingen avklaringsbehov bli opprettet`() {
        val journalpost = TestJournalPost(tema = "AAP", brevkode = Brevkoder.SØKNAD).tilJournalpost()
        every { journalpostRepo.hentHvisEksisterer(behandlingId) } returns journalpost
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns null

        avklarTemaSteg.utfør(kontekst)

        assertThat(InMemoryAvklaringsbehovRepository.hent(behandlingId)).isEmpty()
    }

    @Test
    fun `klage avklares automatisk til tema AAP når feature-toggle er skrudd på`() {
        val journalpost = TestJournalPost(tema = "AAP", brevkode = Brevkoder.KLAGE).tilJournalpost()
        every { journalpostRepo.hentHvisEksisterer(behandlingId) } returns journalpost
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns null
        every { unleashGateway.isEnabled(PostmottakFeature.AutomatiskKlageJournalforing) } returns true

        avklarTemaSteg.utfør(kontekst)

        verify(exactly = 1) { avklarTemaRepository.lagreTemaAvklaring(behandlingId, true, Tema.AAP) }
        assertThat(InMemoryAvklaringsbehovRepository.hent(behandlingId)).isEmpty()
    }

    @Test
    fun `klage krever manuell avklaring av tema når feature-toggle er skrudd av`() {
        val journalpost = TestJournalPost(tema = "AAP", brevkode = Brevkoder.KLAGE).tilJournalpost()
        every { journalpostRepo.hentHvisEksisterer(behandlingId) } returns journalpost
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns null
        every { unleashGateway.isEnabled(PostmottakFeature.AutomatiskKlageJournalforing) } returns false

        avklarTemaSteg.utfør(kontekst)

        verify(exactly = 0) { avklarTemaRepository.lagreTemaAvklaring(any(), any(), any()) }
        assertThat(InMemoryAvklaringsbehovRepository.hent(behandlingId).filter { it.status().erÅpent() }).isNotEmpty()
    }

    @Test
    fun `Når vi ikke kan behandle automatisk og manuell avklaring er avklart med 'skal til AAP' forventer vi at steget ikke returnerer avklaringsbehov`() {
        val journalpost = TestJournalPost(
            tema = "AAP",
            brevkode = Brevkoder.SØKNAD,
            kanal = KanalFraKodeverk.SKAN_NETS
        ).tilJournalpost()
        every { journalpostRepo.hentHvisEksisterer(behandlingId) } returns journalpost

        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns null

        avklarTemaSteg.utfør(kontekst)

        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(true, Tema.AAP)
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandlingId).løsAvklaringsbehov(
            Definisjon.AVKLAR_TEMA,
            begrunnelse = "...",
            endretAv = "Z1234"
        )

        avklarTemaSteg.utfør(kontekst)

        assertThat(InMemoryAvklaringsbehovRepository.hent(behandlingId).filter { it.status().erÅpent() }).isEmpty()
    }

    @Test
    fun `når vi ikke kan behandle automatisk og manuell avklaring mangler forventer vi avklaringsbehov AVKLAR_TEMA`() {
        val journalpost = TestJournalPost(
            tema = "AAP",
            brevkode = Brevkoder.SØKNAD,
            kanal = KanalFraKodeverk.SKAN_NETS
        ).tilJournalpost()
        every { journalpostRepo.hentHvisEksisterer(behandlingId) } returns journalpost
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns null

        avklarTemaSteg.utfør(kontekst)

        assertThat(InMemoryAvklaringsbehovRepository.hent(behandlingId)).hasSize(1)
        assertThat(
            InMemoryAvklaringsbehovRepository.hent(behandlingId).first().definisjon
        ).isEqualTo(Definisjon.AVKLAR_TEMA)
    }

    @Test
    fun `når tema har blitt endret fortsetter vi til neste steg`() {
        val journalpost = TestJournalPost(tema = "ANNET").tilJournalpost()
        every { journalpostRepo.hentHvisEksisterer(behandlingId) } returns journalpost
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(false, Tema.UKJENT)
        every { gosysOppgaveKlient.finnOppgaverForJournalpost(journalpost.journalpostId, tema = "AAP") } returns listOf(
            1,
            2
        )

        avklarTemaSteg.utfør(kontekst)

        verify(exactly = 1) { gosysOppgaveKlient.ferdigstillOppgave(1) }
        verify(exactly = 1) { gosysOppgaveKlient.ferdigstillOppgave(2) }

        assertThat(InMemoryAvklaringsbehovRepository.hent(behandlingId)).isEmpty()
    }

    @Test
    fun `når tema har blitt endret, uten temaavklaring, blir steget fullført`() {
        val journalpost = TestJournalPost(tema = "ANNET").tilJournalpost()
        every { journalpostRepo.hentHvisEksisterer(behandlingId) } returns journalpost
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns null
        every {
            gosysOppgaveKlient.finnOppgaverForJournalpost(
                journalpost.journalpostId,
                tema = "AAP"
            )
        } returns emptyList()

        avklarTemaSteg.utfør(kontekst)

        assertThat(InMemoryAvklaringsbehovRepository.hent(behandlingId)).isEmpty()
    }
}

