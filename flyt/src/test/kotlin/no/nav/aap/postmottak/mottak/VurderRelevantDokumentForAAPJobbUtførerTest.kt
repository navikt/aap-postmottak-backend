package no.nav.aap.postmottak.mottak

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.fordeler.Enhetsutreder
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.ÅrsakTilStatus
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.Bruker
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.ProsesseringsJobber
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.prosessering.TestObjekter.lagTestJournalpost
import no.nav.aap.postmottak.prosessering.medJournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VurderRelevantDokumentForAAPJobbUtførerTest {

    private val journalpostId = JournalpostId(1L)
    private val behandlingId = BehandlingId(42L)

    private val flytJobbRepository = mockk<FlytJobbRepository>(relaxed = true)
    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val journalpostService = mockk<JournalpostService>(relaxed = true)
    private val innkommendeJournalpostRepository = mockk<InnkommendeJournalpostRepository>(relaxed = true)
    private val gosysOppgaveGateway = mockk<GosysOppgaveGateway>(relaxed = true)
    private val enhetsutreder = mockk<Enhetsutreder>(relaxed = true)
    private val meterRegistry = SimpleMeterRegistry()

    private val jobb = VurderRelevantDokumentForAAPJobbUtfører(
        flytJobbRepository,
        behandlingRepository,
        journalpostService,
        innkommendeJournalpostRepository,
        gosysOppgaveGateway,
        enhetsutreder,
        meterRegistry,
    )

    private val input = JobbInput(VurderRelevantDokumentForAAPJobbUtfører)
        .forSak(journalpostId.referanse)
        .medJournalpostId(journalpostId)

    @BeforeEach
    fun setup() {
        every { innkommendeJournalpostRepository.eksisterer(journalpostId) } returns false
        every { behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Fordeling) } returns behandlingId
        every { enhetsutreder.finnJournalføringsenhet(any()) } returns "4491"
    }

    private fun gittSafJournalpost(safJournalpost: SafJournalpost) {
        every { journalpostService.hentSafJournalpost(journalpostId) } returns safJournalpost
    }

    private fun journalpostTeller() = meterRegistry.find("journalpost").counters().sumOf { it.count() }

    @Test
    fun `Gjør ingenting om journalposten allerede er vurdert`() {
        every { innkommendeJournalpostRepository.eksisterer(journalpostId) } returns true

        jobb.utfør(input)

        verify(exactly = 0) { journalpostService.hentSafJournalpost(any()) }
        verify(exactly = 0) { innkommendeJournalpostRepository.lagre(any()) }
        verify(exactly = 0) { gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(any(), any(), any(), any()) }
        verify(exactly = 0) { behandlingRepository.opprettBehandling(any(), any()) }
        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
        assertThat(journalpostTeller()).isEqualTo(0.0)
    }

    @Test
    fun `Normal journalpost lagres som EVALUERT uten regelresultat og starter fordelingsflyt`() {
        gittSafJournalpost(lagTestJournalpost(journalpostId))

        jobb.utfør(input)

        verify(exactly = 1) {
            innkommendeJournalpostRepository.lagre(withArg {
                assertThat(it.journalpostId).isEqualTo(journalpostId)
                assertThat(it.status).isEqualTo(InnkommendeJournalpostStatus.EVALUERT)
                assertThat(it.årsakTilStatus).isNull()
                assertThat(it.regelresultat).isNull()
                assertThat(it.enhet).isEqualTo("4491")
                assertThat(it.brukerId).isEqualTo("fnr")
                assertThat(it.brevkode).isEqualTo(Brevkoder.SØKNAD.kode)
            })
        }
        verify(exactly = 1) { behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Fordeling) }
        verify(exactly = 1) {
            flytJobbRepository.leggTil(withArg {
                assertThat(it.type()).isEqualTo(ProsesserBehandlingJobbUtfører.type)
                assertThat(it.behandlingId()).isEqualTo(behandlingId.id)
                assertThat(it.sakId()).isEqualTo(journalpostId.referanse)
            })
        }
        verify(exactly = 0) { gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(any(), any(), any(), any()) }
        assertThat(journalpostTeller()).isEqualTo(1.0)
    }

    @Test
    fun `Journalført journalpost lagres som IGNORERT og starter ikke flyt`() {
        gittSafJournalpost(lagTestJournalpost(journalpostId).copy(journalstatus = Journalstatus.JOURNALFOERT))

        jobb.utfør(input)

        verifiserLagretOgIngenFlyt(InnkommendeJournalpostStatus.IGNORERT, ÅrsakTilStatus.ALLEREDE_JOURNALFØRT)
        verify(exactly = 0) { gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(any(), any(), any(), any()) }
    }

    @Test
    fun `Utgått journalpost lagres som IGNORERT og starter ikke flyt`() {
        gittSafJournalpost(lagTestJournalpost(journalpostId).copy(journalstatus = Journalstatus.UTGAAR))

        jobb.utfør(input)

        verifiserLagretOgIngenFlyt(InnkommendeJournalpostStatus.IGNORERT, ÅrsakTilStatus.UTGÅTT)
        verify(exactly = 0) { gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(any(), any(), any(), any()) }
    }

    @Test
    fun `Journalpost uten bruker-id gir Gosys fordelingsoppgave uten orgnr`() {
        gittSafJournalpost(lagTestJournalpost(journalpostId).copy(bruker = Bruker(id = null, type = BrukerIdType.FNR)))

        jobb.utfør(input)

        verify(exactly = 1) {
            gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(
                journalpostId = journalpostId,
                personIdent = null,
                orgnr = null,
                beskrivelse = "tittel"
            )
        }
        verifiserLagretOgIngenFlyt(InnkommendeJournalpostStatus.GOSYS_FDR, ÅrsakTilStatus.MANGLER_IDENT, enhet = null)
        verify(exactly = 0) { enhetsutreder.finnJournalføringsenhet(any()) }
    }

    @Test
    fun `Journalpost uten bruker gir Gosys fordelingsoppgave`() {
        gittSafJournalpost(lagTestJournalpost(journalpostId).copy(bruker = null))

        jobb.utfør(input)

        verify(exactly = 1) {
            gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(journalpostId, null, null, "tittel")
        }
        verifiserLagretOgIngenFlyt(InnkommendeJournalpostStatus.GOSYS_FDR, ÅrsakTilStatus.MANGLER_IDENT, enhet = null)
    }

    @Test
    fun `Journalpost med orgnr gir Gosys fordelingsoppgave med orgnr`() {
        gittSafJournalpost(lagTestJournalpost(journalpostId).copy(bruker = Bruker(id = "999999999", type = BrukerIdType.ORGNR)))

        jobb.utfør(input)

        verify(exactly = 1) {
            gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(
                journalpostId = journalpostId,
                personIdent = null,
                orgnr = "999999999",
                beskrivelse = "tittel"
            )
        }
        verifiserLagretOgIngenFlyt(InnkommendeJournalpostStatus.GOSYS_FDR, ÅrsakTilStatus.ORGNR, enhet = null)
        verify(exactly = 0) { enhetsutreder.finnJournalføringsenhet(any()) }
    }

    @Test
    fun `Feiler om journalpost som skal til Gosys mangler dokumenter, uten å lagre`() {
        gittSafJournalpost(lagTestJournalpost(journalpostId).copy(bruker = null, dokumenter = emptyList()))

        assertThatThrownBy { jobb.utfør(input) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Fant ingen dokumenter")

        verify(exactly = 0) { innkommendeJournalpostRepository.lagre(any()) }
        verify(exactly = 0) { behandlingRepository.opprettBehandling(any(), any()) }
    }

    @Test
    fun `Jobben er registrert i ProsesseringsJobber`() {
        assertThat(ProsesseringsJobber.alle().map { it.type }).contains(VurderRelevantDokumentForAAPJobbUtfører.type)
    }

    private fun verifiserLagretOgIngenFlyt(
        status: InnkommendeJournalpostStatus,
        årsak: ÅrsakTilStatus,
        enhet: String? = "4491",
    ) {
        verify(exactly = 1) {
            innkommendeJournalpostRepository.lagre(withArg {
                assertThat(it.journalpostId).isEqualTo(journalpostId)
                assertThat(it.status).isEqualTo(status)
                assertThat(it.årsakTilStatus).isEqualTo(årsak)
                assertThat(it.regelresultat).isNull()
                assertThat(it.enhet).isEqualTo(enhet)
            })
        }
        verify(exactly = 0) { behandlingRepository.opprettBehandling(any(), any()) }
        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
        assertThat(journalpostTeller()).isEqualTo(1.0)
    }
}
