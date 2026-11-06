package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksinfo
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.TemaVurdering
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.FunnetAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.BehandlingsflytSak
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytKlient
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.test.fakes.TestJournalPost
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class AvklarSakStegTest {

    @BeforeEach
    fun setup() {
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns null
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    val behandlingsflytClient = mockk<BehandlingsflytKlient>(relaxed = true)
    val journalpostRepository = mockk<JournalpostRepository>()
    val saksnummerRepository: SaksnummerRepository = mockk(relaxed = true)
    val avklarTemaRepository: AvklarTemaRepository = mockk(relaxed = true)
    val unleashGateway: UnleashGateway = mockk(relaxed = true)

    val avklarSakSteg = AvklarSakSteg(
        saksnummerRepository,
        journalpostRepository,
        behandlingsflytClient, avklarTemaRepository, unleashGateway
    )


    @Test
    fun `når automatisk behandling er mulig etterspørres ny sak uten avklaringsbehov`() {
        val journalpost = TestJournalposter.leggTil().tilJournalpost()

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost
        every { behandlingsflytClient.finnEllerOpprettSak(any(), any()) } returns BehandlingsflytSak(
            "saksnummer", Periode(
                LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1)
            ), null
        )

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 1) { saksnummerRepository.lagreSakVurdering(any(), any()) }

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Test
    fun `når vi ikke kan behandle journalposten automatisk kreves avklaring`() {
        val journalpost = TestJournalposter.papirsøknad().tilJournalpost()

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        every { saksnummerRepository.hentKelvinSaker(any()) } returns listOf(mockk())
        every { saksnummerRepository.hentSakVurdering(any()) } returns null

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }

        assertEquals(FantAvklaringsbehov::class.simpleName, resultat::class.simpleName)
        val funnetAvklaringsbehov = resultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).isEqualTo(Definisjon.AVKLAR_SAK)
    }

    @Test
    fun `når saksnummer er gitt i avklaring går vi videre i flyten`() {
        val journalpost = TestJournalposter.papirsøknad().tilJournalpost()

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        every { saksnummerRepository.hentKelvinSaker(any()) } returns listOf(mockk())

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)

    }

    @ParameterizedTest
    @EnumSource(Tema::class, names = ["AAP", "OPP"], mode = EnumSource.Mode.EXCLUDE)
    fun `går videre uten Kelvin-sak for andre temaer`(tema: Tema) {
        val journalpost = TestJournalposter.papirsøknad().copy(tema = "IKKE APP").tilJournalpost()
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(false, tema)

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }
        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Test
    fun `går videre dersom journalpost er journalført på annet fagsystem`() {
        val journalpost = TestJournalposter.papirsøknad().tilJournalpost().copy(fagsystem = Fagsystem.AO01.name)
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(false, Tema.UKJENT)

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }
        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Test
    fun `dersom journalposten allerede er journalført på Kelvin-sak skal vi lage en saksavklaring med saksnummeret journalposten er journalført på`() {
        val saksnummer = "saksnummer"
        val journalpost = TestJournalposter.papirsøknad().tilJournalpost()
            .copy(fagsystem = Fagsystem.KELVIN.name, saksnummer = saksnummer, status = Journalstatus.JOURNALFOERT)

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 1) {
            saksnummerRepository.lagreSakVurdering(any(), withArg {
                assertThat(it.saksnummer).isEqualTo(saksnummer)
            })
        }

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Test
    fun `legeerklæring med avslag på alle kelvin-saker gir avklaringsbehov`() {
        val journalpost = TestJournalposter.legeerklæring()
            .tilJournalpost()

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost
        every { saksnummerRepository.hentKelvinSaker(any()) } returns listOf(Saksinfo(
            saksnummer = "...",
            periode = Periode(LocalDate.now(), LocalDate.now()),
            avslag = true,
            resultat = null,
            finnesÅpenBehandling = false,
            harRettNåEllerIFramtiden = false
        ))
        every { saksnummerRepository.hentSakVurdering(any()) } returns null

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        assertEquals(FantAvklaringsbehov::class.simpleName, resultat::class.simpleName)
        val funnetAvklaringsbehov = resultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).isEqualTo(Definisjon.AVKLAR_SAK)
    }

    @Test
    fun `klage med nøyaktig én eksisterende kelvin-sak avklares automatisk mot den saken`() {
        val saksnummer = "saksnummer"
        val journalpost = TestJournalPost(brevkode = Brevkoder.KLAGE).tilJournalpost()

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost
        every { saksnummerRepository.hentSakVurdering(any()) } returns null
        every { saksnummerRepository.hentKelvinSaker(any()) } returns listOf(
            Saksinfo(
                saksnummer = saksnummer,
                periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1)),
                harRettNåEllerIFramtiden = false
            )
        )
        every { unleashGateway.isEnabled(PostmottakFeature.AutomatiskKlageJournalforing) } returns true

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 1) {
            saksnummerRepository.lagreSakVurdering(any(), withArg {
                assertThat(it.saksnummer).isEqualTo(saksnummer)
                assertThat(it.opprettetNy).isFalse()
            })
        }
        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Test
    fun `klage uten eksisterende kelvin-sak krever manuell avklaring`() {
        val journalpost = TestJournalPost(brevkode = Brevkoder.KLAGE).tilJournalpost()

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost
        every { saksnummerRepository.hentSakVurdering(any()) } returns null
        every { saksnummerRepository.hentKelvinSaker(any()) } returns emptyList()
        every { unleashGateway.isEnabled(PostmottakFeature.AutomatiskKlageJournalforing) } returns true

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }
        assertEquals(FantAvklaringsbehov::class.simpleName, resultat::class.simpleName)
        val funnetAvklaringsbehov = resultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).isEqualTo(Definisjon.AVKLAR_SAK)
    }

    @Test
    fun `klage med flere eksisterende kelvin-saker krever manuell avklaring`() {
        val periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1))
        val journalpost = TestJournalPost(brevkode = Brevkoder.KLAGE).tilJournalpost()

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost
        every { saksnummerRepository.hentSakVurdering(any()) } returns null
        every { saksnummerRepository.hentKelvinSaker(any()) } returns listOf(
            Saksinfo(saksnummer = "saksnummer-1", periode = periode, harRettNåEllerIFramtiden = false),
            Saksinfo(saksnummer = "saksnummer-2", periode = periode, harRettNåEllerIFramtiden = false)
        )
        every { unleashGateway.isEnabled(PostmottakFeature.AutomatiskKlageJournalforing) } returns true

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }
        assertEquals(FantAvklaringsbehov::class.simpleName, resultat::class.simpleName)
        val funnetAvklaringsbehov = resultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).isEqualTo(Definisjon.AVKLAR_SAK)
    }

    private val periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1))

    private fun sak(saksnummer: String, resultat: ResultatKode? = null) =
        Saksinfo(saksnummer = saksnummer, periode = periode, resultat = resultat, harRettNåEllerIFramtiden = false)

    private fun utførSøknadOmAapIUtlandet(
        kelvinSaker: List<Saksinfo>,
        toggle: Boolean = true,
        sakVurdering: Saksvurdering? = null
    ): StegResultat {
        val journalpost = TestJournalPost(brevkode = Brevkoder.SØKNAD_OM_AAP_I_UTLANDET).tilJournalpost()
        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost
        every { saksnummerRepository.hentSakVurdering(any()) } returns sakVurdering
        every { saksnummerRepository.hentKelvinSaker(any()) } returns kelvinSaker
        every { unleashGateway.isEnabled(PostmottakFeature.AutomatiskJournalforingSoknadUtland) } returns toggle

        return avklarSakSteg.utfør(mockk(relaxed = true))
    }

    private fun assertAvklarSakBehov(resultat: StegResultat) {
        assertEquals(FantAvklaringsbehov::class.simpleName, resultat::class.simpleName)
        assertThat((resultat.transisjon() as FunnetAvklaringsbehov).avklaringsbehov()).isEqualTo(Definisjon.AVKLAR_SAK)
    }

    private fun assertSakValgt(resultat: StegResultat, forventetSaksnummer: String) {
        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 1) {
            saksnummerRepository.lagreSakVurdering(any(), withArg {
                assertThat(it.saksnummer).isEqualTo(forventetSaksnummer)
                assertThat(it.opprettetNy).isFalse()
            })
        }
        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Test
    fun `søknad om AAP i utlandet journalføres på eneste sak`() {
        assertSakValgt(utførSøknadOmAapIUtlandet(listOf(sak("sak-1"))), "sak-1")
    }

    @Test
    fun `søknad om AAP i utlandet journalføres på sak som ikke er trukket når det også finnes en trukket sak`() {
        val resultat = utførSøknadOmAapIUtlandet(listOf(sak("trukket", ResultatKode.TRUKKET), sak("gyldig")))

        assertSakValgt(resultat, "gyldig")
    }

    @Test
    fun `søknad om AAP i utlandet feiler når det finnes flere saker som ikke er trukket`() {
        assertThrows<IllegalStateException> {
            utførSøknadOmAapIUtlandet(listOf(sak("sak-1"), sak("sak-2", ResultatKode.INNVILGET)))
        }

        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }
    }

    @Test
    fun `søknad om AAP i utlandet med bare trukne saker krever manuell avklaring`() {
        val resultat = utførSøknadOmAapIUtlandet(listOf(sak("trukket-1", ResultatKode.TRUKKET), sak("trukket-2", ResultatKode.TRUKKET)))

        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }
        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        assertAvklarSakBehov(resultat)
    }

    @Test
    fun `søknad om AAP i utlandet uten saker krever manuell avklaring`() {
        val resultat = utførSøknadOmAapIUtlandet(emptyList())

        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }
        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        assertAvklarSakBehov(resultat)
    }

    @Test
    fun `søknad om AAP i utlandet med eksisterende saksvurdering lagrer ikke på nytt`() {
        val resultat = utførSøknadOmAapIUtlandet(emptyList(), sakVurdering = Saksvurdering("sak-1", false))

        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }
        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Test
    fun `søknad om AAP i utlandet krever manuell avklaring når feature-toggle er skrudd av`() {
        val resultat = utførSøknadOmAapIUtlandet(listOf(sak("sak-1")), toggle = false)

        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }
        assertAvklarSakBehov(resultat)
    }

    @Test
    fun `ugyldig journalpost for søknad om AAP i utlandet gir ingen sakvalg`() {
        val journalpost = TestJournalPost(brevkode = Brevkoder.SØKNAD_OM_AAP_I_UTLANDET, status = Journalstatus.AVBRUTT)
            .tilJournalpost()
        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost
        every { unleashGateway.isEnabled(PostmottakFeature.AutomatiskJournalforingSoknadUtland) } returns true

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }
        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Test
    fun `søknad om AAP i utlandet som er journalført utenfor Kelvin gir ingen sakvalg`() {
        val journalpost = TestJournalPost(brevkode = Brevkoder.SØKNAD_OM_AAP_I_UTLANDET, status = Journalstatus.JOURNALFOERT)
            .tilJournalpost().copy(fagsystem = Fagsystem.AO01.name)
        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost
        every { unleashGateway.isEnabled(PostmottakFeature.AutomatiskJournalforingSoknadUtland) } returns true

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }
        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

}
