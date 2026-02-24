package no.nav.aap.postmottak.prosessering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.fordeler.RegelRepository
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.TestObjekter.lagTestJournalpost
import no.nav.aap.postmottak.test.Fakes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@Fakes
class FordelingVideresendJobbUtførerTest {
    val behandlingRepositoryMock = mockk<BehandlingRepository>(relaxed = true)
    val regelRepositoryMock = mockk<RegelRepository>()
    val flytJobbRepositoryMock = mockk<FlytJobbRepository>(relaxed = true)
    val journalpostServiceMock = mockk<JournalpostService>(relaxed = true)
    val fordelingVideresendJobbUtfører =
        FordelingVideresendJobbUtfører(
            behandlingRepository = behandlingRepositoryMock,
            regelRepository = regelRepositoryMock,
            flytJobbRepository = flytJobbRepositoryMock,
            arenaVideresender = mockk(),
            journalpostService = journalpostServiceMock
        )

    @Test
    fun `Når journalposten skal til Kelvin skal vi opprette en ProsesserBehandlingJobb`() {
        val testJournalpostId = 1L
        val regelResultat = Regelresultat(
            regelMap = mapOf(
                "Regel1" to true,
                "Regel2" to true,
                "ErIkkeReisestønadRegel" to true,
                "ErIkkeAnkeRegel" to true,
                "KelvinSakRegel" to false,
                "ArenaSakRegel" to false
            ),
            forJournalpost = testJournalpostId
        )
        val journalpostId = JournalpostId(testJournalpostId)
        every { regelRepositoryMock.hentRegelresultat(journalpostId) } returns regelResultat
        every { journalpostServiceMock.hentSafJournalpost(journalpostId) } returns lagTestJournalpost(journalpostId)

        val jobbInput = JobbInput(FordelingVideresendJobbUtfører)
            .forSak(journalpostId.referanse)
            .medJournalpostId(journalpostId)
            .medInnkommendeJournalpostId(testJournalpostId)
        fordelingVideresendJobbUtfører.utfør(jobbInput)

        assertThat(
            fordelingVideresendJobbUtfører.prometheus.counter("fordeling_videresend", "system", "kelvin").count()
        ).isEqualTo(1.0)
        verify(exactly = 1) { behandlingRepositoryMock.opprettBehandling(journalpostId, TypeBehandling.Journalføring) }
        verify(exactly = 1) {
            flytJobbRepositoryMock.leggTil(
                withArg {
                    assertThat(it.sakId()).isEqualTo(journalpostId.referanse)
                    assertThat(it.type()).isEqualTo(ProsesserBehandlingJobbUtfører.type)
                }
            )
        }

    }
}