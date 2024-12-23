package no.nav.aap.postmottak.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.forretningsflyt.steg.SettFagsakSteg
import no.nav.aap.postmottak.gateway.JournalføringsGateway
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import org.junit.jupiter.api.Test

class SettFagsakStegTest {

    val saksnummerRepository: SaksnummerRepository = mockk(relaxed = true)
    val journalpostRepository: JournalpostRepository = mockk()
    val joark: JournalføringsGateway = mockk(relaxed = true)

    val journalføringSteg = SettFagsakSteg(journalpostRepository, saksnummerRepository, joark)

    @Test
    fun `verifiser at journalpost blir oppdatert med saksnummer`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost

        val saksnummer = "saksnummer"
        every { saksnummerRepository.hentSakVurdering(any())?.saksnummer } returns saksnummer

        journalføringSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { joark.førJournalpostPåFagsak(
            journalpost.journalpostId,
            journalpost.person.aktivIdent(),
            saksnummer)
        }
    }
}