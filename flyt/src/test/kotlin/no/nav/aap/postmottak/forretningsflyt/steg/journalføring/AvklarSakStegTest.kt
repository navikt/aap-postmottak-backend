package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.TemaVurdering
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.FunnetAvklaringsbehov
import no.nav.aap.postmottak.gateway.BehandlingsflytSak
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytKlient
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvklarSakStegTest {

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

    @Test
    fun `går videre dersom journalpost ikke har tema AAP`() {
        val journalpost = TestJournalposter.papirsøknad().copy(tema = "IKKE APP").tilJournalpost()
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(false, Tema.UKJENT)

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }
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
    fun `legeerklæring med avslag på alle kelvin-saker gir avklaringsbehov når feature-toggle er skrudd på`() {
        val journalpost = TestJournalposter.legeerklæring()
            .tilJournalpost()

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost
        every { saksnummerRepository.hentKelvinSaker(any()) } returns listOf(mockk {
            every { avslag } returns true
            every { finnesÅpenBehandling } returns false
        })
        every { saksnummerRepository.hentSakVurdering(any()) } returns null
        every { unleashGateway.isEnabled(PostmottakFeature.StoppAutomatikkForLegeerklaringVedAvslag) } returns true

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        assertEquals(FantAvklaringsbehov::class.simpleName, resultat::class.simpleName)
        val funnetAvklaringsbehov = resultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).isEqualTo(Definisjon.AVKLAR_SAK)
    }

    @Test
    fun `legeerklæring med avslag på alle kelvin-saker gir automatisk saksavklaring når feature-toggle er skrudd av`() {
        val journalpost = TestJournalposter.legeerklæring()
            .tilJournalpost()

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost
        every { saksnummerRepository.hentKelvinSaker(any()) } returns listOf(mockk {
            every { avslag } returns true
            every { finnesÅpenBehandling } returns false
        })
        every { behandlingsflytClient.finnEllerOpprettSak(any(), any()) } returns BehandlingsflytSak(
            "saksnummer", Periode(
                LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1)
            ), null
        )
        every { unleashGateway.isEnabled(PostmottakFeature.StoppAutomatikkForLegeerklaringVedAvslag) } returns false

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

}
