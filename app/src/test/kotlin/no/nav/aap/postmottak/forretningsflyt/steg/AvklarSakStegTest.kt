package no.nav.aap.postmottak.forretningsflyt.steg

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.postmottak.klient.joark.Journalpost
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class AvklarSakStegTest {

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    val behandlingsflytClient = mockk<BehandlingsflytClient>(relaxed = true)
    val journalpostRepository = mockk<JournalpostRepositoryImpl>(relaxed = true)
    val saksnummerRepository: SaksnummerRepository = mockk(relaxed = true)

    val avklarSakSteg = AvklarSakSteg(
        saksnummerRepository,
        journalpostRepository,
        behandlingsflytClient)

    @Test
    fun utfør() {
        avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 1) { journalpostRepository.hentHvisEksisterer(any()) }
    }

    @Test
    fun `når det ikke finnes relaterte saker til behandlingen etterspørres ny sak uten avklaringsbehov`() {
        every { saksnummerRepository.hentSaksnummre(any()) } returns emptyList()

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 1) { saksnummerRepository.lagreSakVurdering(any(), any()) }

        assertThat(resultat.avklaringsbehov).isEmpty()
    }

    @Test
    fun `når automatisk behandling er mulig etterspørres ny sak uten avklaringsbehov`() {
        val journalpost: Journalpost.MedIdent = mockk()
        every { journalpost.kanBehandlesAutomatisk() } returns true

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 1) { saksnummerRepository.lagreSakVurdering(any(), any()) }

        assertThat(resultat.avklaringsbehov).isEmpty()
    }

    @Test
    fun `når det finnes relaterte saker til behandlingen kreves avklaring`() {
        val journalpost: Journalpost.MedIdent = mockk()
        every { journalpost.kanBehandlesAutomatisk() } returns false

        every { saksnummerRepository.hentSaksnummre(any()) } returns listOf(mockk())
        every { saksnummerRepository.hentSakVurdering(any()) } returns null

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }

        assertThat(resultat.avklaringsbehov).contains(Definisjon.AVKLAR_SAK)
    }

    @Test
    fun `når det finnes relaterte saker til behandlingen og saksnummer er gitt i avklaring går vi videre i flyten`() {
        val journalpost: Journalpost.MedIdent = mockk()
        every { journalpost.kanBehandlesAutomatisk() } returns false
        every { saksnummerRepository.hentSakVurdering(any())?.opprettNySak } returns false

        every { saksnummerRepository.hentSaksnummre(any()) } returns listOf(mockk())

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 0) { saksnummerRepository.lagreSakVurdering(any(), any()) }

        assertThat(resultat.avklaringsbehov).isEmpty()

    }

    @Test
    fun `når det finnes relaterte saker til behandlingen og avklaring vil opprette nytt saksnummer spør vi behandlingsflyt om saksnummer før vi går videre`() {
        val journalpost: Journalpost.MedIdent = mockk()
        every { journalpost.kanBehandlesAutomatisk() } returns false

        every { saksnummerRepository.hentSakVurdering(any())?.opprettNySak } returns true

        every { saksnummerRepository.hentSaksnummre(any()) } returns listOf(mockk())

        val resultat = avklarSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 1) { saksnummerRepository.lagreSakVurdering(any(), any()) }

        assertThat(resultat.avklaringsbehov).isEmpty()

    }
}