package no.nav.aap.postmottak.forretningsflyt.steg

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.FunnetAvklaringsbehov
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AvklarSakStegTest {

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    val behandlingsflytClient = mockk<BehandlingsflytClient>(relaxed = true)
    val journalpostRepository = mockk<JournalpostRepositoryImpl>()
    val saksnummerRepository: SaksnummerRepository = mockk(relaxed = true)

    val avklarSakSteg = AvklarSakSteg(
        saksnummerRepository,
        journalpostRepository,
        behandlingsflytClient)


    @Test
    fun `når automatisk behandling er mulig etterspørres ny sak uten avklaringsbehov`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.kanBehandlesAutomatisk() } returns true
        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 1) { saksnummerRepository.lagreSakVurdering(any(), any()) }

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Test
    fun `når vi ikke kan behandle journalposten automatisk kreves avklaring`() {
        val journalpost: Journalpost = mockk()
        every { journalpost.kanBehandlesAutomatisk() } returns false
        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        every { saksnummerRepository.hentSaksnummre(any()) } returns listOf(mockk())
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
        every { journalpost.kanBehandlesAutomatisk() } returns false
        every { saksnummerRepository.hentSakVurdering(any())?.opprettNySak } returns false
        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost


        every { saksnummerRepository.hentSaksnummre(any()) } returns listOf(mockk())

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)

    }

    @Test
    fun `når det finnes relaterte saker til behandlingen og avklaring vil opprette nytt saksnummer spør vi behandlingsflyt om saksnummer før vi går videre`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.kanBehandlesAutomatisk() } returns false
        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost


        every { saksnummerRepository.hentSakVurdering(any())?.opprettNySak } returns true

        every { saksnummerRepository.hentSaksnummre(any()) } returns listOf(mockk())

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 1) { saksnummerRepository.lagreSakVurdering(any(), any()) }

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)

    }
}
