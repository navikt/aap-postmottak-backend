package no.nav.aap.behandlingsflyt.forretningsflyt.informasjonskrav.saksnummer

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt.BehandlingsflytGateway
import no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt.Saksinfo
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import org.junit.jupiter.api.Test

class SaksnummerInfoKravTest {

    val sakRepository: SaksnummerRepository = mockk(relaxed = true)
    val behandlingsflytGateway: BehandlingsflytGateway = mockk()

    val saksnummerInfoKrav = SaksnummerInfoKrav(sakRepository, behandlingsflytGateway)

    @Test
    fun `finn saksnummer for saker for borger og lagre på behandling`() {
        val saksnummre: List<Saksnummer> = listOf(Saksnummer("41234"), Saksnummer("45678"))

        every { behandlingsflytGateway.finnSaker(any()) } returns saksnummre.map { Saksinfo(it.toString(), mockk()) }

        saksnummerInfoKrav.harIkkeGjortOppdateringNå(mockk(relaxed = true))

        verify(exactly = 1) { sakRepository.lagreSaksnummer(any(), saksnummre) }
    }
}