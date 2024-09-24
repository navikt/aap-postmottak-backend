package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.behandlingsflyt.joark.Joark
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.junit.jupiter.api.Test

class SettFagsakStegTest {

    val behandlingRepository: BehandlingRepository = mockk(relaxed = true)
    val journalpostRepository: JournalpostRepositoryImpl = mockk()
    val joark: Joark = mockk(relaxed = true)

    val journalføringSteg = SettFagsakSteg(
        behandlingRepository, journalpostRepository, joark
    )

    @Test
    fun `verifiser at journalpost blir oppdatert med saksnummer`() {
        val journalpost: Journalpost.MedIdent = mockk()
        every { journalpostRepository.hentHvisEksisterer(any()) } returns journalpost

        val saksnummer = "saksnummer"
        every { behandlingRepository.hent(any() as BehandlingId ).vurderinger.saksvurdering?.vurdering?.saksnummer } returns saksnummer

        journalføringSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { joark.oppdaterJournalpost(journalpost, saksnummer) }
    }
}