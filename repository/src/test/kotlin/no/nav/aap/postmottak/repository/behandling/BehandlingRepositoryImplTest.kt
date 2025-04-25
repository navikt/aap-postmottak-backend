package no.nav.aap.postmottak.repository.behandling

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class BehandlingRepositoryImplTest {


    @AfterEach
    fun clean() {
        InitTestDatabase.freshDatabase().transaction {
            it.execute("""TRUNCATE BEHANDLING CASCADE""")
        }
    }

    @Test
    fun opprettBehandling() {
        InitTestDatabase.freshDatabase().transaction {
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
        return InitTestDatabase.freshDatabase().transaction {
            val context =
                Context(BehandlingRepositoryImpl(it))
            context.let(block)
        }
    }
}