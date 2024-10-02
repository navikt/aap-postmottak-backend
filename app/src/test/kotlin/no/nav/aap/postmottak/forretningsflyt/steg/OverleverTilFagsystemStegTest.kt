package no.nav.aap.postmottak.forretningsflyt.steg

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Dokument
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.SafRestClient
import no.nav.aap.postmottak.faktagrunnlag.register.behandlingsflyt.BehandlingsflytGateway
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.Dokumentbehandling
import no.nav.aap.postmottak.sakogbehandling.behandling.DokumentbehandlingRepository
import no.nav.aap.verdityper.dokument.DokumentInfoId
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.InputStream

class OverleverTilFagsystemStegTest {

    val dokumentbehandlingRepository: DokumentbehandlingRepository = mockk(relaxed = true)
    val behandlingsflytGateway: BehandlingsflytGateway = mockk(relaxed = true)
    val journalpostRepository: JournalpostRepository = mockk()
    val safRestClient: SafRestClient = mockk(relaxed = true)

    val overførTilFagsystemSteg = OverleverTilFagsystemSteg(
        dokumentbehandlingRepository,
        behandlingsflytGateway,
        journalpostRepository,
        safRestClient
    )


    val kontekst: FlytKontekstMedPerioder = mockk(relaxed = true)
    val journalpost: Journalpost = mockk()
    val journalpostId: JournalpostId = JournalpostId(123)
    val behandling: Dokumentbehandling = mockk()
    val saksnummer = "String"

    @BeforeEach
    fun beforeEach() {
        every { journalpostRepository.hentHvisEksisterer(any()) } returns journalpost
        every { dokumentbehandlingRepository.hentMedLås(any() as BehandlingId, null) } returns behandling
        every { journalpost.journalpostId } returns journalpostId
        every { behandling.journalpostId } returns journalpostId
        every { behandling.vurderinger.saksvurdering?.saksnummer } returns saksnummer
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }


    @Test
    fun `hvis journalpost er manuelt strukturert, blir strukturert dokument sendt til behandlingsflyt`() {

        val kontekst: FlytKontekstMedPerioder = mockk(relaxed = true)

        every { behandling.harBlittStrukturert() } returns true
        every { behandling.vurderinger.struktureringsvurdering?.vurdering } returns "String"

        overførTilFagsystemSteg.utfør(kontekst)

        verify(exactly = 1, inverse = true) { behandling.vurderinger.struktureringsvurdering }
        verify(exactly = 0) { safRestClient.hentDokument(any(), any()) }
        verify(exactly = 1) { behandlingsflytGateway.sendSøknad(saksnummer, journalpostId, any()) }
    }

    @Test
    fun `hvis automatisk journalføring blir strukturert dokument fra joark sendt til behandlingsflyt`() {
        val dokument: Dokument = mockk()
        val dokumentInfoId: DokumentInfoId = mockk()

        every { dokument.dokumentInfoId } returns  dokumentInfoId
        every { behandling.harBlittStrukturert() } returns false
        every { journalpost.finnOriginal() } returns dokument
        every { safRestClient.hentDokument(journalpostId, dokumentInfoId).dokument } returns InputStream.nullInputStream()

        overførTilFagsystemSteg.utfør(kontekst)

        verify(exactly = 0, inverse = true) { behandling.vurderinger.struktureringsvurdering }
        verify(exactly = 1) { safRestClient.hentDokument(journalpostId, dokumentInfoId) }
        verify(exactly = 1) { behandlingsflytGateway.sendSøknad(saksnummer, journalpostId, any()) }
    }
}