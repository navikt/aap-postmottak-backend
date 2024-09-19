package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.junit.jupiter.api.Test

class FinnSakStegTest {

    val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    val behandlingsflytClient = mockk<BehandlingsflytClient>(relaxed = true)
    val journalpostRepository = mockk<JournalpostRepositoryImpl>(relaxed = true)

    val finnSakSteg = FinnSakSteg(behandlingRepository, behandlingsflytClient, journalpostRepository)

    @Test
    fun utfør() {

        finnSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { behandlingRepository.hent(any() as BehandlingId) }
        verify(exactly = 1) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 1) { journalpostRepository.hentHvisEksisterer(any()) }
    }
}