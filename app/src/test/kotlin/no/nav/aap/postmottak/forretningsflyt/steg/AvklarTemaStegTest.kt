package no.nav.aap.postmottak.forretningsflyt.steg

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.sakogbehandling.behandling.Dokumentbehandling
import no.nav.aap.postmottak.sakogbehandling.behandling.DokumentbehandlingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class AvklarTemaStegTest {

    val dokumentbehandlingRepository : DokumentbehandlingRepository = mockk()
    val journalpostRepo : JournalpostRepositoryImpl = mockk()

    val avklarTemaSteg = AvklarTemaSteg(dokumentbehandlingRepository, journalpostRepo)


    val behandling: Dokumentbehandling = mockk()
    val journalpost: Journalpost = mockk(relaxed = true)
    val behandlingId = BehandlingId(10)
    val journalpostId = JournalpostId(11)
    val kontekst = FlytKontekstMedPerioder(behandlingId = behandlingId, TypeBehandling.DokumentHåndtering)


    @BeforeEach
    fun before() {
        every { dokumentbehandlingRepository.hentMedLås(behandlingId, null) } returns behandling
        every { behandling.journalpostId } returns journalpostId
        every { journalpostRepo.hentHvisEksisterer(behandlingId) } returns journalpost
    }

    @AfterEach
    fun after() {
        clearAllMocks()
    }


    @Test
    fun `når automatisk saksbehandling er mulig skal ingen avklaringsbehov bli opprettet`() {
        every { journalpost.kanBehandlesAutomatisk() } returns true

        val actual = avklarTemaSteg.utfør(kontekst)

        assertThat(actual.avklaringsbehov).isEmpty()
    }

    @Test
    fun `når vi ikke kan behandle automatisk og mauell avklaring er utført forventer vi at steget ikke returnerer avklaringsbehov`() {
        every { journalpost.kanBehandlesAutomatisk() } returns false
        every { behandling.harTemaBlittAvklart() } returns true

        val actual = avklarTemaSteg.utfør(kontekst)

        assertThat(actual.avklaringsbehov).isEmpty()
    }

    @Test
    fun `når vi ikke kan behandle automatisk og manuell avklaring mangler forventer vi avklaringsbehov AVKLAR_TEMA`() {
        every { journalpost.kanBehandlesAutomatisk() } returns false
        every { behandling.harTemaBlittAvklart() } returns false

        val actual = avklarTemaSteg.utfør(kontekst)

        assertThat(actual.avklaringsbehov).contains(Definisjon.AVKLAR_TEMA)
    }

}
