package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.TemaVurdering
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.gateway.JournalføringService
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class JournalføringStegTest {

    val saksnummerRepository: SaksnummerRepository = mockk(relaxed = true)
    val journalpostRepository: JournalpostRepository = mockk()
    val joark: JournalføringService = mockk(relaxed = true)
    val avklarTemaRepository: AvklarTemaRepository = mockk(relaxed = true)
    val avklaringsbehovRepository: AvklaringsbehovRepository = mockk(relaxed = true)

    val journalføringSteg = JournalføringSteg(
        journalpostRepository, joark, avklarTemaRepository, avklaringsbehovRepository
    )

    @Test
    fun `verifiser at journalpost blir oppdatert med saksnummer og endelig journalført`() {
        val journalpost = TestJournalposter.leggTil().tilJournalpost()
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(true, Tema.AAP)
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost

        val saksnummer = "saksnummer"
        every { saksnummerRepository.hentSakVurdering(any())?.saksnummer } returns saksnummer
        every { avklaringsbehovRepository.hentAvklaringsbehovene(any()).hvemSomLøste(Definisjon.AVKLAR_SAK) } returns null

        journalføringSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { joark.ferdigstillJournalpostMaskinelt(journalpost.journalpostId, null) }
    }

    @ParameterizedTest
    @EnumSource(Tema::class, names = ["AAP", "OPP"], mode = EnumSource.Mode.EXCLUDE)
    fun `andre temaer ferdigstilles ikke`(tema: Tema) {
        val journalpost = TestJournalposter.leggTil {
            kanal = KanalFraKodeverk.SKAN_NETS
        }.tilJournalpost()
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(false, tema)

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        every { saksnummerRepository.hentSakVurdering(any() as BehandlingId) } throws IllegalStateException("Skal ikke treffe denne mocken")

        val resultat = journalføringSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
        verify(exactly = 0) { joark.ferdigstillJournalpostMaskinelt(any(), any()) }
    }
}