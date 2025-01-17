package no.nav.aap.postmottak.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.TemaVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.forretningsflyt.steg.VideresendSteg
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import org.junit.jupiter.api.Test

class VideresendStegTest {
    val flytJobbRepository: FlytJobbRepository = mockk()
    val saksnummerRepository: SaksnummerRepository = mockk()
    val avklarTemaRepository: AvklarTemaRepository = mockk()


    val videresendSteg =
        VideresendSteg(saksnummerRepository, avklarTemaRepository, mockk(), flytJobbRepository, mockk())

    @Test
    fun `Journalposter som er journalført på generell sak skal ikke videresendes`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        every {avklarTemaRepository.hentTemaAvklaring(any())} returns TemaVurdering(true, Tema.AAP)
        every { saksnummerRepository.hentSakVurdering(any())?.generellSak } returns true
        every { journalpost.tema } returns "AAP"

        videresendSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }

    }
}