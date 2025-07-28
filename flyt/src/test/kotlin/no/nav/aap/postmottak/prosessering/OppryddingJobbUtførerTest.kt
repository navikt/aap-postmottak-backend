package no.nav.aap.postmottak.prosessering

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import kotlin.random.Random
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.Digitaliseringsvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OppryddingJobbUtførerTest {

    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val avklarTemaRepository = mockk<AvklarTemaRepository>(relaxed = true)
    private val digitaliseringsvurderingRepository = mockk<DigitaliseringsvurderingRepository>(relaxed = true)
    private val innkommendeJournalpostRepository = mockk<InnkommendeJournalpostRepository>(relaxed = true)
    private val journalpostGateway = mockk<JournalpostGateway>(relaxed = true)
    private val flytJobbRepository = mockk<FlytJobbRepository>(relaxed = true)

    private val jobb = OppryddingJobbUtfører(
        behandlingRepository,
        avklarTemaRepository,
        digitaliseringsvurderingRepository,
        innkommendeJournalpostRepository,
        journalpostGateway,
        flytJobbRepository
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `skal ikke rydde hvis journalpost ikke er journalført`() {
        val journalpostId = JournalpostId(Random.nextLong())

        every { journalpostGateway.hentJournalpost(journalpostId) } returns mockkJournalpost(Journalstatus.MOTTATT)
        jobb.utfør(jobbInput(journalpostId))

        verify(exactly = 0) { innkommendeJournalpostRepository.eksisterer(any()) }
        verify(exactly = 0) { behandlingRepository.hentAlleBehandlinger(any()) }
    }

    @Test
    fun `skal behandle allerede mottatt journalpost uten digitaliseringsbehandling`() {
        val journalpostId = JournalpostId(Random.nextLong())

        every { journalpostGateway.hentJournalpost(journalpostId) } returns mockkJournalpost(Journalstatus.JOURNALFOERT)
        every { innkommendeJournalpostRepository.eksisterer(journalpostId) } returns true
        every { behandlingRepository.hentAlleBehandlinger(journalpostId) } returns emptyList()

        jobb.utfør(jobbInput(journalpostId))

        verify { behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.DokumentHåndtering) }
    }

    @Test
    fun `skal behandle allerede mottatt journalpost med digitaliseringsbehandling uten vurdering`() {
        val journalpostId = JournalpostId(Random.nextLong())
        val behandlingId = BehandlingId(Random.nextLong())

        val behandling = mockk<Behandling> {
            every { typeBehandling } returns TypeBehandling.DokumentHåndtering
            every { id } returns behandlingId
        }
        every { journalpostGateway.hentJournalpost(journalpostId) } returns mockkJournalpost(Journalstatus.JOURNALFOERT)
        every { innkommendeJournalpostRepository.eksisterer(journalpostId) } returns true
        every { behandlingRepository.hentAlleBehandlinger(journalpostId) } returns listOf(behandling)
        every { digitaliseringsvurderingRepository.hentHvisEksisterer(behandlingId) } returns null

        jobb.utfør(jobbInput(journalpostId))

        verify { behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.DokumentHåndtering) }
    }

    @Test
    fun `skal ikke opprette ny behandling hvis vurdering finnes`() {
        val journalpostId = JournalpostId(Random.nextLong())

        val behandlingId = BehandlingId(Random.nextLong())
        val behandling = mockk<Behandling> {
            every { typeBehandling } returns TypeBehandling.DokumentHåndtering
            every { id } returns behandlingId
        }
        every { journalpostGateway.hentJournalpost(journalpostId) } returns mockkJournalpost(Journalstatus.JOURNALFOERT)
        every { innkommendeJournalpostRepository.eksisterer(journalpostId) } returns true
        every { behandlingRepository.hentAlleBehandlinger(journalpostId) } returns listOf(behandling)
        every { digitaliseringsvurderingRepository.hentHvisEksisterer(behandlingId) } returns Digitaliseringsvurdering(
            InnsendingType.DIALOGMELDING, "{}", LocalDate.now()
        )

        jobb.utfør(jobbInput(journalpostId))

        verify(exactly = 0) { behandlingRepository.opprettBehandling(any(), any()) }
    }


    @Test
    fun `skal opprette behandling for digitalisering hvis ikke allerede mottatt`() {
        val journalpostId = JournalpostId(Random.nextLong())
        val nyBehandlingId = BehandlingId(Random.nextLong())

        every { journalpostGateway.hentJournalpost(journalpostId) } returns mockkJournalpost(Journalstatus.JOURNALFOERT)
        every { innkommendeJournalpostRepository.eksisterer(journalpostId) } returns false
        every { behandlingRepository.opprettBehandling(any(), any()) } returns nyBehandlingId

        jobb.utfør(jobbInput(journalpostId))

        verify {
            behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.DokumentHåndtering)
            flytJobbRepository.leggTil(match { it.behandlingId() == nyBehandlingId.id })
        }
    }

    private fun jobbInput(journalpostId: JournalpostId = JournalpostId(Random.nextLong())) =
        JobbInput(OppryddingJobbUtfører)
            .forSak(journalpostId.referanse)
            .medJournalpostId(journalpostId)

    private fun mockkJournalpost(status: Journalstatus): SafJournalpost = mockk {
        every { journalstatus } returns status
    }
}
