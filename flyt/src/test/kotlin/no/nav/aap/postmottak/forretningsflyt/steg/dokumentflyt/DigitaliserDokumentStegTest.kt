package no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadUtenlandsoppholdV0
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.Digitaliseringsvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksinfo
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.FunnetAvklaringsbehov
import no.nav.aap.postmottak.gateway.DokumentGateway
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.test.fakes.TestJournalPost
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.time.LocalDate

class DigitaliserDokumentStegTest {

    val struktureringsvurderingRepository: DigitaliseringsvurderingRepository = mockk(relaxed = true)
    val journalpostRepo: JournalpostRepository = mockk()
    val dokumentGateway: DokumentGateway = mockk()
    val saksnummerRepository: SaksnummerRepository = mockk()
    val avklaringsbehovRepository: AvklaringsbehovRepository = mockk(relaxed = true)
    val unleashGateway: UnleashGateway = mockk(relaxed = true)

    val digitaliserDokumentSteg = DigitaliserDokumentSteg(
        struktureringsvurderingRepository, journalpostRepo, dokumentGateway,saksnummerRepository, avklaringsbehovRepository, unleashGateway
    )

    @Test
    fun `når behandlingen må gjøres manuelt og strukturering ikke er gjort forventes et nytt avlaringsbehov for strukturering`() {
        val journalpost = TestJournalposter.papirsøknad().tilJournalpost()

        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { saksnummerRepository.eksistererAvslagPåTidligereBehandling(any<BehandlingId>()) } returns false
        every { saksnummerRepository.hentKelvinSaker(any<BehandlingId>()) } returns emptyList()

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(FantAvklaringsbehov::class.simpleName, stegresultat::class.simpleName)
        val funnetAvklaringsbehov = stegresultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).isEqualTo(Definisjon.DIGITALISER_DOKUMENT)

    }

    @Test
    fun `når behandlingen må gjøres manuelt og strukturering er utført forventes ingen avklaringsbehov`() {
        val journalpost = TestJournalposter.papirsøknad().tilJournalpost()

        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns mockk(relaxed = true)
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { saksnummerRepository.eksistererAvslagPåTidligereBehandling(any<BehandlingId>()) } returns false

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, stegresultat::class.simpleName)
    }

    @Test
    fun `når behandling kan gjøres automatisk og strukturering ikke er gjort forventes ingen avklaringsbehov`() {
        val journalpost = TestJournalposter.digitalSøknad().tilJournalpost()
        val journalpostJson = """{
            |"yrkesskade": "Nei",
            |"student": {"erStudent": "Nei", "kommeTilbake": "Nei"},
            |"oppgitteBarn": {"identer": [], "barn": []}
            |}""".trimMargin()

        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { saksnummerRepository.eksistererAvslagPåTidligereBehandling(any<BehandlingId>()) } returns false
        every { saksnummerRepository.hentKelvinSaker(any<BehandlingId>()) } returns emptyList()

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
        val journalpost = TestJournalposter.leggTil().tilJournalpost()

        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { saksnummerRepository.eksistererAvslagPåTidligereBehandling(any<BehandlingId>()) } returns false
        every { saksnummerRepository.hentKelvinSaker(any<BehandlingId>()) } returns emptyList()

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, stegresultat::class.simpleName)
    }

    @Test
    fun `digital legeerklæring med avslag på alle kelvin-saker skal ikke digitaliseres automatisk`() {
        val journalpost = TestJournalposter.leggTil().tilJournalpost()

        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { saksnummerRepository.eksistererAvslagPåTidligereBehandling(any<BehandlingId>()) } returns false
        every { saksnummerRepository.hentKelvinSaker(any<BehandlingId>()) } returns listOf(Saksinfo(
            saksnummer = "...",
            periode = Periode(LocalDate.now(), LocalDate.now()),
            avslag = true,
            resultat = ResultatKode.AVSLAG,
            finnesÅpenBehandling = false,
            harRettNåEllerIFramtiden = false
        ))

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(FantAvklaringsbehov::class.simpleName, stegresultat::class.simpleName)
        val funnetAvklaringsbehov = stegresultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).isEqualTo(Definisjon.DIGITALISER_DOKUMENT)
    }

    @Test
    fun `lager manuell digitaliseringsoppgave hvis barn oppgitt i søknad har ugyldig ident`() {
        val journalpost = TestJournalposter.digitalSøknad().tilJournalpost()
        val journalpostJson = """{
            |"yrkesskade": "Nei",
            |"student": {"erStudent": "Nei", "kommeTilbake": "Nei"},
            |"oppgitteBarn": {"identer": [], "barn": [{"navn": "barn", "fødselsdato": "2022-12-12", "ident": {"identifikator": "123456"}, "relasjon": "FORELDER"}]}
            |}""".trimMargin()

        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { saksnummerRepository.eksistererAvslagPåTidligereBehandling(any<BehandlingId>()) } returns false
        every { saksnummerRepository.hentKelvinSaker(any<BehandlingId>()) } returns emptyList()
        every {
            dokumentGateway.hentDokument(
                journalpost.journalpostId,
                any()
            ).dokument
        } returns ByteArrayInputStream(journalpostJson.toByteArray())

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(FantAvklaringsbehov::class.simpleName, stegresultat::class.simpleName)
        val funnetAvklaringsbehov = stegresultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).isEqualTo(Definisjon.DIGITALISER_DOKUMENT)

        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns mockk(relaxed = true)
        val stegresultatIgjen = digitaliserDokumentSteg.utfør(mockk(relaxed = true))
        assertEquals(Fullført::class.simpleName, stegresultatIgjen::class.simpleName)
    }

    @Test
    fun `klage med avklart eksisterende sak digitaliseres automatisk`() {
        val journalpost = TestJournalPost(brevkode = Brevkoder.KLAGE).tilJournalpost()

        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { saksnummerRepository.eksistererAvslagPåTidligereBehandling(any<BehandlingId>()) } returns false
        every { saksnummerRepository.hentSakVurdering(any<BehandlingId>()) } returns Saksvurdering(
            saksnummer = "saksnummer",
            opprettetNy = false
        )
        every { unleashGateway.isEnabled(PostmottakFeature.AutomatiskKlageJournalforing) } returns true

        val digitaliseringsvurderingSlot = slot<Digitaliseringsvurdering>()
        every {
            struktureringsvurderingRepository.lagre(any(), capture(digitaliseringsvurderingSlot))
        } just runs

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, stegresultat::class.simpleName)
        assertThat(digitaliseringsvurderingSlot.captured.kategori).isEqualTo(InnsendingType.KLAGE)
        assertThat(digitaliseringsvurderingSlot.captured.strukturertDokument).isNotNull
    }

    @Test
    fun `klage uten avklart sak forventes et nytt avklaringsbehov for strukturering`() {
        val journalpost = TestJournalPost(brevkode = Brevkoder.KLAGE).tilJournalpost()

        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { saksnummerRepository.eksistererAvslagPåTidligereBehandling(any<BehandlingId>()) } returns false
        every { saksnummerRepository.hentSakVurdering(any<BehandlingId>()) } returns null

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(FantAvklaringsbehov::class.simpleName, stegresultat::class.simpleName)
        val funnetAvklaringsbehov = stegresultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).isEqualTo(Definisjon.DIGITALISER_DOKUMENT)
    }

    @Test
    fun `klage knyttet til nyopprettet sak digitaliseres ikke automatisk`() {
        val journalpost = TestJournalPost(brevkode = Brevkoder.KLAGE).tilJournalpost()

        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { saksnummerRepository.eksistererAvslagPåTidligereBehandling(any<BehandlingId>()) } returns false
        every { saksnummerRepository.hentSakVurdering(any<BehandlingId>()) } returns Saksvurdering(
            saksnummer = "saksnummer",
            opprettetNy = true
        )

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(FantAvklaringsbehov::class.simpleName, stegresultat::class.simpleName)
        val funnetAvklaringsbehov = stegresultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).isEqualTo(Definisjon.DIGITALISER_DOKUMENT)
    }

    private fun utlandsøknad() = TestJournalPost(brevkode = Brevkoder.SØKNAD_OM_AAP_I_UTLANDET).tilJournalpost()

    private fun klargjørUtlandsøknad(
        toggle: Boolean = true,
        journalpost: no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost = utlandsøknad(),
    ) {
        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns null
        every { journalpostRepo.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { saksnummerRepository.eksistererAvslagPåTidligereBehandling(any<BehandlingId>()) } returns false
        every { saksnummerRepository.hentKelvinSaker(any<BehandlingId>()) } returns emptyList()
        every { unleashGateway.isEnabled(PostmottakFeature.AutomatiskJournalforingSoknadUtland) } returns toggle
    }

    @Test
    fun `søknad om AAP i utlandet digitaliseres automatisk`() {
        klargjørUtlandsøknad()
        val digitaliseringsvurderingSlot = slot<Digitaliseringsvurdering>()
        every {
            struktureringsvurderingRepository.lagre(any(), capture(digitaliseringsvurderingSlot))
        } just runs

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, stegresultat::class.simpleName)
        val vurdering = digitaliseringsvurderingSlot.captured
        assertThat(vurdering.kategori).isEqualTo(InnsendingType.SØKNAD_UTENLANDSOPPHOLD)
        assertThat(vurdering.digitalisertManueltGjennomPostmottak).isFalse
        val melding = DefaultJsonMapper.fromJson<SøknadUtenlandsoppholdV0>(vurdering.strukturertDokument!!)
        assertThat(melding.mottattDato).isEqualTo(utlandsøknad().mottattDato)
    }

    @Test
    fun `søknad om AAP i utlandet med toggle av reiser manuell digitalisering`() {
        klargjørUtlandsøknad(toggle = false)

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        val funnetAvklaringsbehov = stegresultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).isEqualTo(Definisjon.DIGITALISER_DOKUMENT)
        verify(exactly = 0) { struktureringsvurderingRepository.lagre(any(), any()) }
    }

    @Test
    fun `søknad om AAP i utlandet digitaliseres automatisk uavhengig av hvordan saken ble avklart`() {
        klargjørUtlandsøknad()
        every { struktureringsvurderingRepository.lagre(any(), any()) } just runs
        listOf(
            null,
            Saksvurdering(saksnummer = "saksnummer", opprettetNy = false),
            Saksvurdering(saksnummer = "saksnummer", opprettetNy = true),
        ).forEach { saksvurdering ->
            every { saksnummerRepository.hentSakVurdering(any<BehandlingId>()) } returns saksvurdering

            val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

            assertEquals(Fullført::class.simpleName, stegresultat::class.simpleName)
        }
        verify(exactly = 3) { struktureringsvurderingRepository.lagre(any(), any()) }
    }

    @Test
    fun `søknad om AAP i utlandet på papir digitaliseres automatisk`() {
        val papir = TestJournalPost(
            brevkode = Brevkoder.SØKNAD_OM_AAP_I_UTLANDET,
            kanal = KanalFraKodeverk.SKAN_NETS
        ).tilJournalpost()
        klargjørUtlandsøknad(journalpost = papir)

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, stegresultat::class.simpleName)
        verify(exactly = 1) { struktureringsvurderingRepository.lagre(any(), any()) }
    }

    @Test
    fun `ugyldig journalpost med søknad om AAP i utlandet digitaliseres ikke`() {
        val ugyldig = TestJournalPost(
            brevkode = Brevkoder.SØKNAD_OM_AAP_I_UTLANDET,
            status = no.nav.aap.postmottak.gateway.Journalstatus.UTGAAR
        ).tilJournalpost()
        klargjørUtlandsøknad(journalpost = ugyldig)

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, stegresultat::class.simpleName)
        verify(exactly = 0) { struktureringsvurderingRepository.lagre(any(), any()) }
    }

    @Test
    fun `ettersending til søknad om AAP i utlandet (NAVe 11-03_07, brevkode ANNEN) digitaliseres ikke automatisk`() {
        val ettersending = TestJournalPost(brevkode = Brevkoder.ANNEN).tilJournalpost()
        klargjørUtlandsøknad(journalpost = ettersending)

        val stegresultat = digitaliserDokumentSteg.utfør(mockk(relaxed = true))

        val funnetAvklaringsbehov = stegresultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).isEqualTo(Definisjon.DIGITALISER_DOKUMENT)
    }
}
