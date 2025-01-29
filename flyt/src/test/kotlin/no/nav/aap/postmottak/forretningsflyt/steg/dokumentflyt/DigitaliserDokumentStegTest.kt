package no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.FunnetAvklaringsbehov
import no.nav.aap.postmottak.gateway.DokumentGateway
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class DigitaliserDokumentStegTest {

    val struktureringsvurderingRepository: DigitaliseringsvurderingRepository = mockk(relaxed = true)
    val journalpostRepo: JournalpostRepository = mockk()
    val dokumentGateway: DokumentGateway = mockk()

    val digitaliserDokumentSteg = DigitaliserDokumentSteg(
        struktureringsvurderingRepository, journalpostRepo, dokumentGateway
    )

    @Test
    fun `når behandlingen må gjøres manuelt og strukturering ikke er gjort forventes et nytt avlaringsbehov for strukturering`() {
        val journalpost: Journalpost = mockk(relaxed = true)

        every { journalpost.erDigitalSøknad() } returns false
        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(FantAvklaringsbehov::class.simpleName, stegresultat::class.simpleName)
        val funnetAvklaringsbehov = stegresultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).contains(Definisjon.DIGITALISER_DOKUMENT)

    }

    @Test
    fun `når behandlingen må gjøres manuelt og strukturering er utført forventes ingen avklaringsbehov`() {
        val journalpost: Journalpost = mockk(relaxed = true)

        every { journalpost.erDigitalSøknad() } returns false
        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns mockk(relaxed = true)
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, stegresultat::class.simpleName)
    }

    @Test
    fun `når behandling kan gjøres automatisk og strukturering ikke er gjort forventes ingen avklaringsbehov`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        val journalpostJson = """{
            |"yrkesskade": "Nei",
            |"student": {"erStudent": "Nei", "kommeTilbake": "Nei"},
            |"oppgitteBarn": {"identer": []}
            |}""".trimMargin()

        every { journalpost.erDigitalSøknad() } returns true
        every { journalpost.journalpostId }
        every { journalpost.hoveddokumentbrevkode } returns Brevkoder.SØKNAD.kode
        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every {
            dokumentGateway.hentDokument(
                journalpost.journalpostId,
                any()
            ).dokument
        } returns ByteArrayInputStream(journalpostJson.toByteArray())
        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, stegresultat::class.simpleName)

    }

    @Test
    fun `digital legeerklæring skal ikke digitaliseres`() {
        val journalpost: Journalpost = mockk(relaxed = true)

        every { journalpost.erDigitalLegeerklæring() } returns true
        every { journalpost.hoveddokumentbrevkode } returns Brevkoder.LEGEERKLÆRING.kode
        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, stegresultat::class.simpleName)
    }

}
