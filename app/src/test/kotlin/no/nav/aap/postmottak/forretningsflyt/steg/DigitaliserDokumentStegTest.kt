package no.nav.aap.postmottak.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandling
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DigitaliserDokumentStegTest {

    val behandlingRepository: BehandlingRepository = mockk()
    val journalpostRepo: JournalpostRepositoryImpl = mockk()

    val digitaliserDokumentSteg = DigitaliserDokumentSteg(
        behandlingRepository, journalpostRepo
    )

    @Test
    fun `når behandlingen må gjøres manuelt og strukturering ikke er gjort forventes et nytt avlaringsbehov for strukturering`() {

        val behandling: Behandling = mockk(relaxed = true)
        val journalpost: Journalpost.MedIdent = mockk(relaxed = true)

        every { journalpost.kanBehandlesAutomatisk() } returns false
        every { behandling.harBlittStrukturert() } returns false
        every { journalpostRepo.hentHvisEksisterer(any()) } returns journalpost
        every { behandlingRepository.hent(any()as BehandlingId) } returns behandling

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertThat(stegresultat.avklaringsbehov).contains(Definisjon.DIGITALISER_DOKUMENT)

    }

    @Test
    fun `når behandlingen må gjøres manuelt og strukturering er utført forventes ingen avklaringsbehov`() {

        val behandling: Behandling = mockk(relaxed = true)
        val journalpost: Journalpost.MedIdent = mockk(relaxed = true)

        every { journalpost.kanBehandlesAutomatisk() } returns false
        every { behandling.harBlittStrukturert() } returns true
        every { journalpostRepo.hentHvisEksisterer(any()) } returns journalpost
        every { behandlingRepository.hent(any()as BehandlingId) } returns behandling

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertThat(stegresultat.avklaringsbehov).isEmpty()

    }

    @Test
    fun `når behandling kan gjøres automatisk og strukturering ikke er gjort forventes ingen avklaringsbehov`() {

        val behandling: Behandling = mockk(relaxed = true)
        val journalpost: Journalpost.MedIdent = mockk(relaxed = true)

        every { journalpost.kanBehandlesAutomatisk() } returns true
        every { behandling.harBlittStrukturert() } returns false
        every { journalpostRepo.hentHvisEksisterer(any()) } returns journalpost
        every { behandlingRepository.hent(any()as BehandlingId) } returns behandling

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertThat(stegresultat.avklaringsbehov).isEmpty()

    }

}