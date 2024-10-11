package no.nav.aap.postmottak.vurdering

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AvklaringRepositoryImplTest {

    @BeforeEach
    fun clean() {
        InitTestDatabase.dataSource.transaction {
            it.execute("""TRUNCATE BEHANDLING CASCADE""")
        }
    }

    @Test
    fun `når struktureringsvurdering blir lagret forventer jeg å finne den på behandlingen`() {
        inContext {

            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111))

            val json = """{"Test: Dokument"}"""
            avklaringRepository.lagreStrukturertDokument(behandlingId, json)

            val strukturertBehandling = behandlingRepository.hent(behandlingId)

            assertThat(strukturertBehandling.harBlittStrukturert()).isTrue()
            assertThat(strukturertBehandling.vurderinger.struktureringsvurdering?.vurdering).isEqualTo(json)

        }
    }

    @Test
    fun `når to struktureringsvurderinger blir lagret forventer jeg å finne den siste på behandlingen`() {
        val json = """{"Test: Dokument"}"""

        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreStrukturertDokument(behandlingId, """{"Test: Plakat"}""") }
        Thread.sleep(100)
        inContext { avklaringRepository.lagreStrukturertDokument(behandlingId, json) }
        inContext {
            val behandlingMedTemavurdering = behandlingRepository.hent(behandlingId)

            assertThat(behandlingMedTemavurdering.vurderinger.struktureringsvurdering?.vurdering).isEqualTo(json)
        }
    }

    private class Context(
        val avklaringRepository: AvklaringRepository,
        val behandlingRepository: BehandlingRepository
    )

    private fun <T>inContext(block: Context.() -> T): T {
        return InitTestDatabase.dataSource.transaction {
            val context = Context(AvklaringRepositoryImpl(it), BehandlingRepositoryImpl(it))
            context.let(block)
        }
    }
}