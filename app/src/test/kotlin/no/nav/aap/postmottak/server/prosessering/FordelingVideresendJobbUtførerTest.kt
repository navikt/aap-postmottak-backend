package no.nav.aap.postmottak.server.prosessering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.fordeler.RegelRepository
import no.nav.aap.postmottak.fordeler.Regelresultat
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.test.fakes.WithFakes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FordelingVideresendJobbUtførerTest : WithFakes {
    val behandlingRepositoryMock = mockk<BehandlingRepository>(relaxed = true)
    val regelRepositoryMock = mockk<RegelRepository>()
    val flytJobbRepositoryMock = mockk<FlytJobbRepository>(relaxed = true)
    val fordelingVideresendJobbUtfører =
        FordelingVideresendJobbUtfører(behandlingRepositoryMock, regelRepositoryMock, flytJobbRepositoryMock, mockk(relaxed = true))

    @Test
    fun `Når journalposten skal til Kelvin skal vi opprette en ProsesserBehandlingJobb`() {
        val regelResultat = Regelresultat(regelMap = mapOf("Regel1" to true, "Regel2" to true))
        every { regelRepositoryMock.hentRegelresultat(any()) } returns regelResultat

        val journalpostId = JournalpostId(1)
        val jobbInput = JobbInput(FordelingVideresendJobbUtfører)
            .medJournalpostId(journalpostId)
        fordelingVideresendJobbUtfører.utfør(jobbInput)


        verify(exactly = 1) { behandlingRepositoryMock.opprettBehandling(journalpostId, TypeBehandling.Journalføring) }
        verify(exactly = 1) {
            flytJobbRepositoryMock.leggTil(
                withArg{
                    assertThat(it.sakId()).isEqualTo(journalpostId.referanse)
                    assertThat(it.type()).isEqualTo(ProsesserBehandlingJobbUtfører.type())
                }
            )
        }

    }
}