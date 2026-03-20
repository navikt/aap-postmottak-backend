package no.nav.aap.postmottak.prosessering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class FeilregistrertJournalpostJobbUtførerTest {

    private val behandlingRepository: BehandlingRepository = mockk(relaxed = true)
    private val overleveringVurderingRepository: OverleveringVurderingRepository = mockk(relaxed = true)
    private val saksnummerRepository: SaksnummerRepository = mockk(relaxed = true)
    private val behandlingsflytGateway: BehandlingsflytGateway = mockk(relaxed = true)

    private val feilregistrertJournalpostJobbUtfører = FeilregistrertJournalpostJobbUtfører(
        behandlingRepository,
        overleveringVurderingRepository,
        saksnummerRepository,
        behandlingsflytGateway
    )

    @Test
    fun `sender feilregistrert-hendelse til behandlingsflyt når journalpost ble overlevert til Kelvin`() {
        val journalpostId = JournalpostId(123L)
        val behandlingId = BehandlingId(1L)
        val saksnummer = "SAK123"

        val dokumentflytBehandling = lagBehandling(
            id = behandlingId,
            journalpostId = journalpostId,
            typeBehandling = TypeBehandling.DokumentHåndtering,
            status = Status.AVSLUTTET
        )

        every { behandlingRepository.hentAlleBehandlingerForSak(journalpostId) } returns listOf(dokumentflytBehandling)
        every { overleveringVurderingRepository.hentHvisEksisterer(behandlingId) } returns OverleveringVurdering(skalOverleveresTilKelvin = true)
        every { saksnummerRepository.hentSakVurdering(behandlingId) } returns Saksvurdering(saksnummer = saksnummer)

        feilregistrertJournalpostJobbUtfører.utfør(
            JobbInput(FeilregistrertJournalpostJobbUtfører)
                .forSak(journalpostId.referanse)
                .medJournalpostId(journalpostId)
        )

        verify(exactly = 1) {
            behandlingsflytGateway.sendFeilregistrertHendelse(
                journalpostId = journalpostId,
                saksnummer = saksnummer
            )
        }
    }

    @Test
    fun `sender ikke hendelse når overleveringVurdering ikke finnes`() {
        val journalpostId = JournalpostId(123L)
        val behandlingId = BehandlingId(1L)

        val dokumentflytBehandling = lagBehandling(
            id = behandlingId,
            journalpostId = journalpostId,
            typeBehandling = TypeBehandling.DokumentHåndtering,
            status = Status.AVSLUTTET
        )

        every { behandlingRepository.hentAlleBehandlingerForSak(journalpostId) } returns listOf(dokumentflytBehandling)
        every { overleveringVurderingRepository.hentHvisEksisterer(behandlingId) } returns null

        feilregistrertJournalpostJobbUtfører.utfør(
            JobbInput(FeilregistrertJournalpostJobbUtfører)
                .forSak(journalpostId.referanse)
                .medJournalpostId(journalpostId)
        )

        verify(exactly = 0) {
            behandlingsflytGateway.sendFeilregistrertHendelse(any(), any())
        }
    }

    @Test
    fun `sender ikke hendelse når skalOverleveresTilKelvin er false`() {
        val journalpostId = JournalpostId(123L)
        val behandlingId = BehandlingId(1L)

        val dokumentflytBehandling = lagBehandling(
            id = behandlingId,
            journalpostId = journalpostId,
            typeBehandling = TypeBehandling.DokumentHåndtering,
            status = Status.AVSLUTTET
        )

        every { behandlingRepository.hentAlleBehandlingerForSak(journalpostId) } returns listOf(dokumentflytBehandling)
        every { overleveringVurderingRepository.hentHvisEksisterer(behandlingId) } returns OverleveringVurdering(skalOverleveresTilKelvin = false)

        feilregistrertJournalpostJobbUtfører.utfør(
            JobbInput(FeilregistrertJournalpostJobbUtfører)
                .forSak(journalpostId.referanse)
                .medJournalpostId(journalpostId)
        )

        verify(exactly = 0) {
            behandlingsflytGateway.sendFeilregistrertHendelse(any(), any())
        }
    }

    @Test
    fun `sender ikke hendelse når ingen dokumentflyt-behandling finnes`() {
        val journalpostId = JournalpostId(123L)
        val behandlingId = BehandlingId(1L)

        val journalføringsbehandling = lagBehandling(
            id = behandlingId,
            journalpostId = journalpostId,
            typeBehandling = TypeBehandling.Journalføring,
            status = Status.AVSLUTTET
        )

        every { behandlingRepository.hentAlleBehandlingerForSak(journalpostId) } returns listOf(journalføringsbehandling)

        feilregistrertJournalpostJobbUtfører.utfør(
            JobbInput(FeilregistrertJournalpostJobbUtfører)
                .forSak(journalpostId.referanse)
                .medJournalpostId(journalpostId)
        )

        verify(exactly = 0) {
            behandlingsflytGateway.sendFeilregistrertHendelse(any(), any())
        }
    }

    @Test
    fun `sender ikke hendelse når saksnummer ikke finnes`() {
        val journalpostId = JournalpostId(123L)
        val behandlingId = BehandlingId(1L)

        val dokumentflytBehandling = lagBehandling(
            id = behandlingId,
            journalpostId = journalpostId,
            typeBehandling = TypeBehandling.DokumentHåndtering,
            status = Status.AVSLUTTET
        )

        every { behandlingRepository.hentAlleBehandlingerForSak(journalpostId) } returns listOf(dokumentflytBehandling)
        every { overleveringVurderingRepository.hentHvisEksisterer(behandlingId) } returns OverleveringVurdering(skalOverleveresTilKelvin = true)
        every { saksnummerRepository.hentSakVurdering(behandlingId) } returns null

        feilregistrertJournalpostJobbUtfører.utfør(
            JobbInput(FeilregistrertJournalpostJobbUtfører)
                .forSak(journalpostId.referanse)
                .medJournalpostId(journalpostId)
        )

        verify(exactly = 0) {
            behandlingsflytGateway.sendFeilregistrertHendelse(any(), any())
        }
    }

    @Test
    fun `finner riktig dokumentflyt-behandling når det finnes både journalføring og dokumenthåndtering`() {
        val journalpostId = JournalpostId(123L)
        val journalføringsBehandlingId = BehandlingId(1L)
        val dokumentflytBehandlingId = BehandlingId(2L)
        val saksnummer = "SAK456"

        val journalføringsbehandling = lagBehandling(
            id = journalføringsBehandlingId,
            journalpostId = journalpostId,
            typeBehandling = TypeBehandling.Journalføring,
            status = Status.AVSLUTTET
        )

        val dokumentflytBehandling = lagBehandling(
            id = dokumentflytBehandlingId,
            journalpostId = journalpostId,
            typeBehandling = TypeBehandling.DokumentHåndtering,
            status = Status.AVSLUTTET
        )

        every { behandlingRepository.hentAlleBehandlingerForSak(journalpostId) } returns listOf(journalføringsbehandling, dokumentflytBehandling)
        every { overleveringVurderingRepository.hentHvisEksisterer(dokumentflytBehandlingId) } returns OverleveringVurdering(skalOverleveresTilKelvin = true)
        every { saksnummerRepository.hentSakVurdering(dokumentflytBehandlingId) } returns Saksvurdering(saksnummer = saksnummer)

        feilregistrertJournalpostJobbUtfører.utfør(
            JobbInput(FeilregistrertJournalpostJobbUtfører)
                .forSak(journalpostId.referanse)
                .medJournalpostId(journalpostId)
        )

        verify(exactly = 1) {
            behandlingsflytGateway.sendFeilregistrertHendelse(
                journalpostId = journalpostId,
                saksnummer = saksnummer
            )
        }

        // Verifiser at vi ikke sjekket journalføringsbehandlingen
        verify(exactly = 0) {
            overleveringVurderingRepository.hentHvisEksisterer(journalføringsBehandlingId)
        }
    }

    private fun lagBehandling(
        id: BehandlingId,
        journalpostId: JournalpostId,
        typeBehandling: TypeBehandling,
        status: Status
    ): Behandling {
        return Behandling(
            id = id,
            journalpostId = journalpostId,
            status = status,
            opprettetTidspunkt = LocalDateTime.now(),
            referanse = BehandlingsreferansePathParam(UUID.randomUUID()),
            typeBehandling = typeBehandling
        )
    }
}



