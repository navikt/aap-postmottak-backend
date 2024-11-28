package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytKlient
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytSak
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SaksnummerInfoKravTest {

    val sakRepository: SaksnummerRepository = mockk(relaxed = true)
    val behandlingsflytKlient: BehandlingsflytKlient = mockk()
    val journalpostRepository: JournalpostRepository = mockk(relaxed = true)

    val saksnummerInfoKrav = SaksnummerInfoKrav(sakRepository, behandlingsflytKlient, journalpostRepository)

    @Test
    fun `finn saksnummer for saker for borger og lagre på behandling`() {
        val saksnummre: List<BehandlingsflytSak> = listOf(
            BehandlingsflytSak("1234", getPeriode()),
            BehandlingsflytSak("5678", getPeriode())
        )

        every { behandlingsflytKlient.finnSaker(any()) } returns saksnummre

        saksnummerInfoKrav.oppdater(mockk(relaxed = true))

        verify(exactly = 1) { sakRepository.lagreSaksnummer(any(), saksnummre.map { it.tilSaksinfo() }) }
    }

    private fun getPeriode() = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 31))

}