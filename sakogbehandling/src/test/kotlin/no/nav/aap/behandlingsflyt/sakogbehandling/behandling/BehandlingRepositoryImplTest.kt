package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BehandlingRepositoryImplTest {

    @Test
    fun opprettBehandling() {
        InitTestDatabase.dataSource.transaction {
            val repository = BehandlingRepositoryImpl(it)

            val journalpostId = JournalpostId(11111)
            val behandling = repository.opprettBehandling(journalpostId)

            assertThat(behandling.journalpostId).isEqualTo(journalpostId)
            assertThat(behandling.typeBehandling).isEqualTo(TypeBehandling.DokumentHåndtering)
        }
    }

    @Test
    fun `når grovvurdering blir lagret forventer jeg å finne den på behandlingen`() {
        InitTestDatabase.dataSource.transaction {
            val repository = BehandlingRepositoryImpl(it)

            val behandlingId = repository.opprettBehandling(JournalpostId(11111)).id

            repository.lagreGrovvurdeing(behandlingId, false)

            val behandlingMedGrovvurdering = repository.hent(behandlingId)

            assertThat(behandlingMedGrovvurdering.harBlittgrovkategorisert()).isTrue()
            assertThat(behandlingMedGrovvurdering.vurderinger.grovkategorivurdering?.vurdering).isFalse()

        }
    }

    @Test
    fun `når kategoriseringvurdering blir lagret forventer jeg å finne den på behandlingen`() {
        InitTestDatabase.dataSource.transaction {
            val repository = BehandlingRepositoryImpl(it)

            val behandlingId = repository.opprettBehandling(JournalpostId(11111)).id

            repository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)

            val behandlingMedKategorisering = repository.hent(behandlingId)

            assertThat(behandlingMedKategorisering.harBlittKategorisert()).isTrue()
            assertThat(behandlingMedKategorisering.vurderinger.kategorivurdering?.vurdering).isEqualTo(Brevkode.SØKNAD)

        }
    }

    @Test
    fun `når struktureringsvurdering blir lagret forventer jeg å finne den på behandlingen`() {
        InitTestDatabase.dataSource.transaction {
            val repository = BehandlingRepositoryImpl(it)

            val behandlingId = repository.opprettBehandling(JournalpostId(11111)).id

            val json = """{"Test: Dokument"}"""
            repository.lagreStrukturertDokument(behandlingId, json)

            val behandlingMedGrovvurdering = repository.hent(behandlingId)

            assertThat(behandlingMedGrovvurdering.harBlittStrukturert()).isTrue()
            assertThat(behandlingMedGrovvurdering.vurderinger.struktureringsvurdering?.vurdering).isEqualTo(json)

        }
    }

    @Test
    fun hent() {
    }

    @Test
    fun hentBehandlingType() {
    }

    @Test
    fun testHent() {
    }
}