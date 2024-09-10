package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mottak.saf.SafGraphqlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.SafRestClient
import no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt.BehandlingsflytGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.junit.jupiter.api.Test
import java.io.InputStream

class OverleverTilFagsystemStegTest {

    val behandlingRepository: BehandlingRepository = mockk(relaxed = true)
    val behandlingsflytGateway: BehandlingsflytGateway = mockk(relaxed = true)
    val safGraphqlGateway: SafGraphqlGateway = mockk(relaxed = true)
    val safRestClient: SafRestClient = mockk(relaxed = true)

    val overførTilFagsystemSteg = OverleverTilFagsystemSteg(
        behandlingRepository,
        behandlingsflytGateway,
        safGraphqlGateway,
        safRestClient
    )

    @Test
    fun `hvis manuell strukturering blir strukturert dokument sendt til behandlingsflyt`() {

        val kontekst: FlytKontekstMedPerioder = mockk(relaxed = true)

        val behandling: Behandling = mockk(relaxed = true)

        every { behandling.harBlittStrukturert() } returns true
        every { behandling.vurderinger.struktureringsvurdering!!.vurdering } returns "String"
        every { behandlingRepository.hent(any() as BehandlingId) } returns behandling

        overførTilFagsystemSteg.utfør(kontekst)

        verify(exactly = 1) { behandling.vurderinger.struktureringsvurdering }
        verify(exactly = 0) { safRestClient.hentDokument(any(), any()) }
        verify(exactly = 1) { behandlingsflytGateway.sendSøknad(any(), any(), any()) }
    }

    @Test
    fun `hvis automatisk journalføring blir strukturert dokument fra joark sendt til behandlingsflyt`() {

        val kontekst: FlytKontekstMedPerioder = mockk(relaxed = true)

        val behandling: Behandling = mockk(relaxed = true)

        every { behandling.harBlittStrukturert() } returns false
        every { behandlingRepository.hent(any() as BehandlingId) } returns behandling
        every { safRestClient.hentDokument(any(), any()).dokument } returns InputStream.nullInputStream()

        overførTilFagsystemSteg.utfør(kontekst)

        verify(exactly = 0) { behandling.vurderinger.struktureringsvurdering }
        verify(exactly = 1) { safRestClient.hentDokument(any(), any()) }
        verify(exactly = 1) { behandlingsflytGateway.sendSøknad(any(), any(), any()) }
    }
}