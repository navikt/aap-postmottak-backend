package no.nav.aap.postmottak.repository.behandling

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.StegTilstand
import no.nav.aap.postmottak.journalpostogbehandling.flyt.StegStatus
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BehandlingRepositoryImplTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()


    @Test
    fun opprettBehandling() {
        dataSource.transaction {
            val repository = BehandlingRepositoryImpl(it)

            val journalpostId = JournalpostId(11111)
            val behandlingId = repository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)

            assertThat(behandlingId).isNotNull
        }
    }

    @Test
    fun `hent behandling med id returnerer behandling`() {
        inContext {
            val behandlingId =
                behandlingRepository.opprettBehandling(JournalpostId(11111), TypeBehandling.Journalføring)

            val hentetBehandling = behandlingRepository.hent(behandlingId)

            assertThat(hentetBehandling).isNotNull()
        }
    }

    @Test
    fun `legge til historikk og hente ut`() {
        inContext {
            val behandlingId =
                behandlingRepository.opprettBehandling(JournalpostId(11111), TypeBehandling.Journalføring)

            behandlingRepository.loggBesøktSteg(
                behandlingId, StegTilstand(
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    stegType = StegType.DIGITALISER_DOKUMENT
                )
            )

            val b = behandlingRepository.hent(behandlingId)
            assertThat(b.stegHistorikk()).containsExactly(
                StegTilstand(stegStatus = StegStatus.AVKLARINGSPUNKT, stegType = StegType.DIGITALISER_DOKUMENT)
            )
        }
    }

    @Test
    fun `hent behandling med journalpostId returnerer behandling`() {
        inContext {
            val behandlingsreferanse = JournalpostId(11111)
            val behandlingId =
                behandlingRepository.opprettBehandling(behandlingsreferanse, TypeBehandling.Journalføring)

            val hentetBehandling = behandlingRepository.hent(behandlingId)

            assertThat(hentetBehandling).isNotNull()
        }
    }

    @Test
    fun `hent hent åpen journalføringsehandling`() {
        val journalpostId = JournalpostId(1)
        inContext {
            behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
            val actual = behandlingRepository.hentÅpenJournalføringsbehandling(journalpostId)
            assertThat(actual).isNotNull
        }
    }


    private class Context(
        val behandlingRepository: BehandlingRepository,
    )

    private fun <T> inContext(block: Context.() -> T): T {
        return dataSource.transaction {
            val context =
                Context(BehandlingRepositoryImpl(it))
            context.let(block)
        }
    }
}