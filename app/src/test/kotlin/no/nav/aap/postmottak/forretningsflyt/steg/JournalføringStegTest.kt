package no.nav.aap.postmottak.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.joark.Joark
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.postmottak.sakogbehandling.behandling.DokumentbehandlingRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.junit.jupiter.api.Test

class JournalføringStegTest {

    val dokumentbehandlingRepository: DokumentbehandlingRepository = mockk(relaxed = true)
    val journalpostRepository: JournalpostRepositoryImpl = mockk()
    val joark: Joark = mockk(relaxed = true)

    val journalføringSteg = JournalføringSteg(
        journalpostRepository, joark
    )

    @Test
    fun `verifiser at journalpost blir oppdatert med saksnummer og endelig journalført`() {
        val journalpost: Journalpost.MedIdent = mockk()
        every { journalpostRepository.hentHvisEksisterer(any()) } returns journalpost

        val saksnummer = "saksnummer"
        every { dokumentbehandlingRepository.hentMedLås(any() as BehandlingId, null).vurderinger.saksvurdering?.saksnummer } returns saksnummer

        journalføringSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { joark.ferdigstillJournalpostMaskinelt(journalpost) }
    }
}