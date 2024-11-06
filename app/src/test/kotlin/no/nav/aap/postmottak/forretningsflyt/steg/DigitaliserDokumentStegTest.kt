package no.nav.aap.postmottak.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.FunnetAvklaringsbehov
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DigitaliserDokumentStegTest {

    val struktureringsvurderingRepository: StruktureringsvurderingRepository = mockk()
    val journalpostRepo: JournalpostRepositoryImpl = mockk()

    val digitaliserDokumentSteg = DigitaliserDokumentSteg(
        struktureringsvurderingRepository, journalpostRepo
    )

    @Test
    fun `når behandlingen må gjøres manuelt og strukturering ikke er gjort forventes et nytt avlaringsbehov for strukturering`() {
        val journalpost: Journalpost = mockk(relaxed = true)

        every { journalpost.kanBehandlesAutomatisk() } returns false
        every { struktureringsvurderingRepository.hentStruktureringsavklaring(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        val funnetAvklaringsbehov = stegresultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).contains(Definisjon.DIGITALISER_DOKUMENT)

    }

    @Test
    fun `når behandlingen må gjøres manuelt og strukturering er utført forventes ingen avklaringsbehov`() {
        val journalpost: Journalpost= mockk(relaxed = true)

        every { journalpost.kanBehandlesAutomatisk() } returns false
        every { struktureringsvurderingRepository.hentStruktureringsavklaring(any()) } returns mockk(relaxed = true)
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertThat(stegresultat is Fullført)
    }

    @Test
    fun `når behandling kan gjøres automatisk og strukturering ikke er gjort forventes ingen avklaringsbehov`() {
        val journalpost: Journalpost = mockk(relaxed = true)

        every { journalpost.kanBehandlesAutomatisk() } returns true
        every { struktureringsvurderingRepository.hentStruktureringsavklaring(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertThat(stegresultat is Fullført)

    }

}
