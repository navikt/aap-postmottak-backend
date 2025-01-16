package no.nav.aap.postmottak.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.forretningsflyt.steg.VideresendSteg
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import org.junit.jupiter.api.Test

class VideresendStegTest {
    val journalpostRepository: JournalpostRepository = mockk()
    val flytJobbRepository: FlytJobbRepository = mockk()
    val saksnummerRepository: SaksnummerRepository = mockk()


    val videresendSteg =
        VideresendSteg(saksnummerRepository, mockk(), mockk(), flytJobbRepository, journalpostRepository, mockk())

    @Test
    fun `Journalposter som er journalført på generell sak skal ikke videresendes`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost
        every { saksnummerRepository.hentSakVurdering(any())?.generellSak } returns true
        every { journalpost.tema } returns "AAP"

        videresendSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }

    }
}