package no.nav.aap.postmottak

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class BehandlingRepositoryImplTest {


    @AfterEach
    fun clean() {
        InitTestDatabase.dataSource.transaction {
            it.execute("""TRUNCATE BEHANDLING CASCADE""")
        }
    }

    @Test
    fun opprettBehandling() {
        InitTestDatabase.dataSource.transaction {
            val repository = BehandlingRepositoryImpl(it)

            val journalpostId = JournalpostId(11111)
            val behandlingId = repository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)

            assertThat(behandlingId).isNotNull
        }
    }

    @Test
    fun `hent behandling med id returnerer behandling`() {
        inContext {
            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111), TypeBehandling.Journalføring)

            val hentetBehandling = behandlingRepository.hent(behandlingId)

            assertThat(hentetBehandling).isNotNull()
        }
    }

    @Test
    fun `hent behandling med journalpostId returnerer behandling`() {
        inContext {
            val behandlingsreferanse = JournalpostId(11111)
            val behandlingId = behandlingRepository.opprettBehandling(behandlingsreferanse, TypeBehandling.Journalføring)

            val hentetBehandling = behandlingRepository.hent(behandlingId)

            assertThat(hentetBehandling).isNotNull()
        }
    }


    private class Context(
        val behandlingRepository: BehandlingRepository,
    )

    private fun <T> inContext(block: Context.() -> T): T {
        return InitTestDatabase.dataSource.transaction {
            val context =
                Context(BehandlingRepositoryImpl(it))
            context.let(block)
        }
    }
}