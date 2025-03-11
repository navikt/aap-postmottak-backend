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
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytKlient
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class AvklarSakStegTest {

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    val behandlingsflytClient = mockk<BehandlingsflytKlient>(relaxed = true)
    val journalpostRepository = mockk<JournalpostRepository>()
    val saksnummerRepository: SaksnummerRepository = mockk(relaxed = true)
    val avklarTemaRepository: AvklarTemaRepository = mockk(relaxed = true)

    val avklarSakSteg = AvklarSakSteg(
        saksnummerRepository,
        journalpostRepository,
        behandlingsflytClient, avklarTemaRepository
    )


    @Test
    fun `når automatisk behandling er mulig etterspørres ny sak uten avklaringsbehov`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.erDigitalSøknad() } returns true
        every { journalpost.tema } returns "AAP"
        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost
        every { behandlingsflytClient.finnEllerOpprettSak(any(), any()) } returns BehandlingsflytSak(
            "saksnummer", Periode(
                LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1)
            )
        )

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 1) { saksnummerRepository.lagreSakVurdering(any(), any()) }

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Test
    fun `når vi ikke kan behandle journalposten automatisk kreves avklaring`() {
        val journalpost: Journalpost = mockk()
        every { journalpost.erDigitalSøknad() } returns false
        every { journalpost.erDigitalLegeerklæring() } returns false
        every { journalpost.erDigitaltMeldekort() } returns false
        every { journalpost.erUgyldig() } returns false
        every { journalpost.erDigitalKlage() } returns false
        every { journalpost.tema } returns "AAP"
        every { journalpost.status } returns Journalstatus.MOTTATT

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        every { saksnummerRepository.hentKelvinSaker(any()) } returns listOf(mockk())
        every { saksnummerRepository.hentSakVurdering(any()) } returns null

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }

        assertEquals(FantAvklaringsbehov::class.simpleName, resultat::class.simpleName)
        val funnetAvklaringsbehov = resultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).contains(Definisjon.AVKLAR_SAK)
    }

    @Test
    fun `når saksnummer er gitt i avklaring går vi videre i flyten`() {
        val journalpost: Journalpost = mockk()
        every { journalpost.erDigitalSøknad() } returns false
        every { journalpost.erDigitalLegeerklæring() } returns false
        every { journalpost.erDigitaltMeldekort() } returns false
        every { journalpost.tema } returns "AAP"
        every { journalpost.erUgyldig() } returns false
        every { journalpost.status } returns Journalstatus.MOTTATT

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        every { saksnummerRepository.hentKelvinSaker(any()) } returns listOf(mockk())

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)

    }

    @Test
    fun `går videre dersom journalpost ikke har tema AAP`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.erDigitalSøknad() } returns false
        every { journalpost.erDigitalLegeerklæring() } returns false
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(false, Tema.UKJENT)
        every { journalpost.tema } returns "ikke AAP"

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }
        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Test
    fun `går videre dersom journalpost er journalført på annet fagsystem`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.erDigitalSøknad() } returns false
        every { journalpost.erDigitalLegeerklæring() } returns false
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(false, Tema.UKJENT)
        every { journalpost.tema } returns Tema.AAP.name
        every { journalpost.fagsystem } returns Fagsystem.AO01.name

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }
        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Test
    fun `dersom journalposten allerede er journalført på Kelvin-sak skal vi lage en saksavklaring med saksnummeret journalposten er journalført på`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        val saksnummer = "saksnummer"
        every { journalpost.erDigitalSøknad() } returns false
        every { journalpost.erDigitalLegeerklæring() } returns false
        every { journalpost.erUgyldig() } returns false
        every { journalpost.status } returns Journalstatus.JOURNALFOERT
        every { journalpost.tema } returns "AAP"
        every { journalpost.saksnummer } returns saksnummer
        every { journalpost.fagsystem } returns Fagsystem.KELVIN.name

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 1) {
            saksnummerRepository.lagreSakVurdering(any(), withArg {
                assertThat(it.saksnummer).isEqualTo(saksnummer.toString())
            })
        }

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Disabled("Automatisk behandling kan ikke skje så lenge det er manuell rutine for å skrive brev om mottatt klage")
    @Test
    fun `Hvis det kun finnes én sak kan klage behandles automatisk`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.erDigitalSøknad() } returns false
        every { journalpost.erDigitalLegeerklæring() } returns false
        every { journalpost.erDigitaltMeldekort() } returns false
        every { journalpost.erDigitalKlage() } returns true
        every { journalpost.tema } returns "AAP"
        every { journalpost.erUgyldig() } returns false
        every { journalpost.status } returns Journalstatus.MOTTATT
        every { journalpost.person } returns Person(1L, UUID.randomUUID(), listOf(Ident("ident")))

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        every { saksnummerRepository.hentKelvinSaker(any()) } returns listOf(mockk())
        every { saksnummerRepository.hentSakVurdering(any()) } returns null

        every { behandlingsflytClient.finnEllerOpprettSak(any(), any()) } returns BehandlingsflytSak(
            "saksnummer", Periode(
                LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1)
            )
        )
        
        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 1) { saksnummerRepository.lagreSakVurdering(any(), any()) }

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)

    }
}
