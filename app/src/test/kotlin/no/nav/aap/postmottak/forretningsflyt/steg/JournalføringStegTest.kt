package no.nav.aap.postmottak.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.klient.joark.Joark
import no.nav.aap.postmottak.klient.joark.Journalpost
import org.junit.jupiter.api.Test

class JournalføringStegTest {

    val saksnummerRepository: SaksnummerRepository = mockk(relaxed = true)
    val journalpostRepository: JournalpostRepositoryImpl = mockk()
    val joark: Joark = mockk(relaxed = true)

    val journalføringSteg = JournalføringSteg(
        journalpostRepository, saksnummerRepository, joark
    )

    @Test
    fun `verifiser at journalpost blir oppdatert med saksnummer og endelig journalført`() {
        val journalpost: Journalpost.MedIdent = mockk()
        every { journalpostRepository.hentHvisEksisterer(any()) } returns journalpost

        val saksnummer = "saksnummer"
        every { saksnummerRepository.hentSakVurdering(any())?.saksnummer } returns saksnummer

        journalføringSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { joark.ferdigstillJournalpostMaskinelt(journalpost) }
    }
}