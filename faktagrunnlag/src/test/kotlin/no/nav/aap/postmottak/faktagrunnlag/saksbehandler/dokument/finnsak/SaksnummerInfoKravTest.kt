package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.register.behandlingsflyt.BehandlingsflytGateway
import org.junit.jupiter.api.Test

class SaksnummerInfoKravTest {

    val sakRepository: SaksnummerRepository = mockk(relaxed = true)
    val behandlingsflytGateway: BehandlingsflytGateway = mockk()
    val journalpostRepository: JournalpostRepository = mockk(relaxed = true)

    val saksnummerInfoKrav = SaksnummerInfoKrav(sakRepository, behandlingsflytGateway, journalpostRepository)

    @Test
    fun `finn saksnummer for saker for borger og lagre på behandling`() {
        val saksnummre: List<Saksinfo> = listOf(mockk(), mockk())

        every { behandlingsflytGateway.finnSaker(any()) } returns saksnummre

        saksnummerInfoKrav.harIkkeGjortOppdateringNå(mockk(relaxed = true))

        verify(exactly = 1) { sakRepository.lagreSaksnummer(any(), saksnummre) }
    }
}