package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.joark.Joark
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.behandlingsflyt.saf.graphql.SafGraphqlGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import org.junit.jupiter.api.Test

class JournalføringStegTest {

    val behandlingRepository: BehandlingRepository = mockk(relaxed = true)
    val safGraphqlGateway: SafGraphqlGateway = mockk()
    val joark: Joark = mockk(relaxed = true)

    val journalføringSteg = JournalføringSteg(
        behandlingRepository, safGraphqlGateway, joark
    )

    @Test
    fun utfør() {
        val journalpost: Journalpost = mockk()
        every { safGraphqlGateway.hentJournalpost(any()) } returns journalpost

        journalføringSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) { joark.oppdaterJournalpost(journalpost as Journalpost.MedIdent, any()) }
        verify(exactly = 1) { joark.ferdigstillJournalpost(journalpost) }

    }
}