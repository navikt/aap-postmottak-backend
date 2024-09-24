package no.nav.aap.postmottak.forretningsflyt.steg

import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.register.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl
import org.junit.jupiter.api.Test

class FinnSakStegTest {

    val behandlingRepository = mockk<BehandlingRepositoryImpl>(relaxed = true)
    val avklaringRepository = mockk<AvklaringRepositoryImpl>(relaxed = true)
    val behandlingsflytClient = mockk<BehandlingsflytClient>(relaxed = true)
    val journalpostRepository = mockk<JournalpostRepositoryImpl>(relaxed = true)
    val saksnummerRepository: SaksnummerRepository = mockk(relaxed = true)

    val finnSakSteg = FinnSakSteg(
        behandlingRepository,
        avklaringRepository,
        saksnummerRepository,
        journalpostRepository,
        behandlingsflytClient)

    @Test
    fun utfør() {

        finnSakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { behandlingsflytClient.finnEllerOpprettSak(any(), any()) }
        verify(exactly = 1) { journalpostRepository.hentHvisEksisterer(any()) }
    }
}