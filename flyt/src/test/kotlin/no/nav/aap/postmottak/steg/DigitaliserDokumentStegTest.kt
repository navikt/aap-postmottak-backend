package no.nav.aap.postmottak.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.FunnetAvklaringsbehov
import no.nav.aap.postmottak.forretningsflyt.steg.DigitaliserDokumentSteg
import no.nav.aap.postmottak.gateway.DokumentGateway
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class DigitaliserDokumentStegTest {

    val struktureringsvurderingRepository: StruktureringsvurderingRepository = mockk(relaxed = true)
    val journalpostRepo: JournalpostRepository = mockk()
    val kategorivurderingRepo: KategoriVurderingRepository = mockk()
    val dokumentGateway: DokumentGateway = mockk()

    val digitaliserDokumentSteg = DigitaliserDokumentSteg(
        struktureringsvurderingRepository, journalpostRepo, kategorivurderingRepo, dokumentGateway
    )

    @Test
    fun `når behandlingen må gjøres manuelt og strukturering ikke er gjort forventes et nytt avlaringsbehov for strukturering`() {
        val journalpost: Journalpost = mockk(relaxed = true)

        every { journalpost.erDigitalSøknad() } returns false
        every { struktureringsvurderingRepository.hentStruktureringsavklaring(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { kategorivurderingRepo.hentKategoriAvklaring(any()) } returns KategoriVurdering(InnsendingType.SØKNAD)

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(FantAvklaringsbehov::class.simpleName, stegresultat::class.simpleName)
        val funnetAvklaringsbehov = stegresultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).contains(Definisjon.DIGITALISER_DOKUMENT)

    }

    @Test
    fun `når behandlingen må gjøres manuelt og strukturering er utført forventes ingen avklaringsbehov`() {
        val journalpost: Journalpost = mockk(relaxed = true)

        every { journalpost.erDigitalSøknad() } returns false
        every { struktureringsvurderingRepository.hentStruktureringsavklaring(any()) } returns mockk(relaxed = true)
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { kategorivurderingRepo.hentKategoriAvklaring(any()) } returns mockk(relaxed = true)

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
        every { struktureringsvurderingRepository.hentStruktureringsavklaring(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { kategorivurderingRepo.hentKategoriAvklaring(any()) } returns mockk(relaxed = true)
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
    fun `når behandlingen har kategori som ikke skal struktureres forventes ingen avklaringsbehov`() {
        val journalpost: Journalpost = mockk(relaxed = true)

        every { journalpost.erDigitalSøknad() } returns false
        every { struktureringsvurderingRepository.hentStruktureringsavklaring(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { kategorivurderingRepo.hentKategoriAvklaring(any()) } returns KategoriVurdering(InnsendingType.LEGEERKLÆRING)

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, stegresultat::class.simpleName)
    }

    @Test
    fun `digital legeerklæring skal ikke digitaliseres`() {
        val journalpost: Journalpost = mockk(relaxed = true)

        every { journalpost.erDigitalLegeerklæring() } returns false
        every { struktureringsvurderingRepository.hentStruktureringsavklaring(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { kategorivurderingRepo.hentKategoriAvklaring(any()) } returns KategoriVurdering(InnsendingType.LEGEERKLÆRING)

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, stegresultat::class.simpleName)
    }

}
