package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.VurderOpprettelseAvSakRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.VurderOpprettelseAvSakVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.TemaVurdering
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.gateway.JournalføringService
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.VurderOpprettelseAvSakValg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JournalføringStegTest {

    val saksnummerRepository: SaksnummerRepository = mockk(relaxed = true)
    val journalpostRepository: JournalpostRepository = mockk()
    val joark: JournalføringService = mockk(relaxed = true)
    val avklarTemaRepository: AvklarTemaRepository = mockk(relaxed = true)
    val vurderOpprettelseAvSakRepository: VurderOpprettelseAvSakRepository = mockk(relaxed = true)

    val journalføringSteg = JournalføringSteg(
        journalpostRepository, joark, avklarTemaRepository, vurderOpprettelseAvSakRepository
    )

    @Test
    fun `verifiser at journalpost blir oppdatert med saksnummer og endelig journalført`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(true, Tema.AAP)
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost

        val saksnummer = "saksnummer"
        every { saksnummerRepository.hentSakVurdering(any())?.saksnummer } returns saksnummer

        journalføringSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { joark.ferdigstillJournalpostMaskinelt(journalpost.journalpostId) }
    }

    @Test
    fun `går videre dersom journalpost ikke har tema AAP`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.erDigitalSøknad() } returns false
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(false, Tema.UKJENT)

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        every { saksnummerRepository.hentSakVurdering(any() as BehandlingId) } throws IllegalStateException("Skal ikke treffe denne mocken")

        val resultat = journalføringSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Test
    fun `journalpost som skal til Arena ferdigstilles ikke i Kelvin`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.erUgyldig() } returns false
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { vurderOpprettelseAvSakRepository.hentHvisEksisterer(any()) } returns
            VurderOpprettelseAvSakVurdering(valg = VurderOpprettelseAvSakValg.ARENA)

        val resultat = journalføringSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
        verify(exactly = 0) { joark.ferdigstillJournalpostMaskinelt(any()) }
    }
}