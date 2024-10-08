package no.nav.aap.postmottak.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.sakogbehandling.behandling.JournalpostRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.Journalpost
import no.nav.aap.postmottak.joark.Joark
import no.nav.aap.postmottak.sakogbehandling.behandling.DokumentbehandlingRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.junit.jupiter.api.Test

class SettFagsakStegTest {

    val dokumentbehandlingRepository: DokumentbehandlingRepository = mockk(relaxed = true)
    val joark: Joark = mockk(relaxed = true)

    val journalføringSteg = SettFagsakSteg(
        dokumentbehandlingRepository, joark
    )

    @Test
    fun `verifiser at journalpost blir oppdatert med saksnummer`() {
        val journalpost: Journalpost.MedIdent = mockk()
        every { dokumentbehandlingRepository.hentMedLås(any() as BehandlingId).journalpost } returns journalpost


        val saksnummer = "saksnummer"
        every { dokumentbehandlingRepository.hentMedLås(any() as BehandlingId, null).vurderinger.saksvurdering?.saksnummer } returns saksnummer


        journalføringSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { joark.førJournalpostPåFagsak(journalpost, saksnummer) }
    }
}