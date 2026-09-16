package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.TemaVurdering
import org.junit.jupiter.api.Test

class VideresendStegTest {
    val flytJobbRepository: FlytJobbRepository = mockk()
    val saksnummerRepository: SaksnummerRepository = mockk()
    val avklarTemaRepository: AvklarTemaRepository = mockk()
    val journalpostRepository: JournalpostRepository = mockk(relaxed = true)

    val videresendSteg =
        VideresendSteg(
            saksnummerRepository,
            avklarTemaRepository,
            mockk(),
            journalpostRepository,
            flytJobbRepository,
            mockk()
        )

    @Test
    fun `Journalposter som er journalført på generell sak skal ikke videresendes`() {
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(true, Tema.AAP)
        every { saksnummerRepository.hentSakVurdering(any())?.generellSak } returns true

        videresendSteg.utfør(mockk(relaxed = true))

        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }

    }
}