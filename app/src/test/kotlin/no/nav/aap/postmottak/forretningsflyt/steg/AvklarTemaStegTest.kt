package no.nav.aap.postmottak.forretningsflyt.steg

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam.AvklarTemaRepository
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.FunnetAvklaringsbehov
import no.nav.aap.postmottak.klient.gosysoppgave.Oppgaveklient
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class AvklarTemaStegTest {

    val avklarTemaRepository: AvklarTemaRepository = mockk()
    val journalpostRepo: JournalpostRepositoryImpl = mockk()
    val oppgaveklient: Oppgaveklient = mockk(relaxed = true)

    val avklarTemaSteg = AvklarTemaSteg(journalpostRepo, avklarTemaRepository, oppgaveklient)


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
        every { journalpost.kanBehandlesAutomatisk() } returns true
        every { journalpost.tema } returns "AAP"

        val actual = avklarTemaSteg.utfør(kontekst)

        assertThat(actual is Fullført)
    }

    @Test
    fun `naar vi ikke kan behandle automatisk og manuell avklaring er avklart med 'skal til AAP' forventer vi at steget ikke returnerer avklaringsbehov`() {
        every { journalpost.kanBehandlesAutomatisk() } returns false
        every { avklarTemaRepository.hentTemaAvklaring(any())?.skalTilAap } returns true

        val actual = avklarTemaSteg.utfør(kontekst)

        assertThat(actual is Fullført)
    }

    @Test
    fun `når vi ikke kan behandle automatisk og manuell avklaring mangler forventer vi avklaringsbehov AVKLAR_TEMA`() {
        every { journalpost.kanBehandlesAutomatisk() } returns false
        every { journalpost.tema } returns "AAP"
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns null

        val actual = avklarTemaSteg.utfør(kontekst)

        val funnetAvklaringsbehov = actual.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).contains(Definisjon.AVKLAR_TEMA)
    }

}
