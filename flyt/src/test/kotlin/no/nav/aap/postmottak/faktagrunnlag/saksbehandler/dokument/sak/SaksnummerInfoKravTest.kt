package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.BehandlingsflytSaksInfoTilPostmottak
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SaksnummerInfoKravTest {

    val sakRepository: SaksnummerRepository = mockk(relaxed = true)
    val behandlingsflytKlient: BehandlingsflytGateway = mockk()
    val journalpostRepository: JournalpostRepository = mockk(relaxed = true)

    val saksnummerInfoKrav = SaksnummerInfoKrav(sakRepository, behandlingsflytKlient, journalpostRepository)

    @Test
    fun `finn saksnummer for saker for borger og lagre på behandling`() {
        val saksnummre: List<BehandlingsflytSaksInfoTilPostmottak> = listOf(
            BehandlingsflytSaksInfoTilPostmottak("1234", getPeriode(), null, finnesÅpenBehandling = true),
            BehandlingsflytSaksInfoTilPostmottak("5678", getPeriode(), null, finnesÅpenBehandling = true)
        )

        every { behandlingsflytKlient.finnSaker(any()) } returns saksnummre

        saksnummerInfoKrav.oppdater(mockk(relaxed = true))

        verify(exactly = 1) { sakRepository.lagreKelvinSak(any(), saksnummre.map { it.tilSaksinfo() }) }
    }

    private fun getPeriode() = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 31))

}