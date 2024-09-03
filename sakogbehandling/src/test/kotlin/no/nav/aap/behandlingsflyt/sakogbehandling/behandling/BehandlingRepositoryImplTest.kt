package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
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
    fun `når to grovvurdering blir lagret forventer jeg å finne den siste på behandlingen`() {
        val behandlingId = transactionMedBehandlingRepository { it.opprettBehandling(JournalpostId(1)).id }
        transactionMedBehandlingRepository { it.lagreGrovvurdeing(behandlingId, false) }
        Thread.sleep(100)
        transactionMedBehandlingRepository { it.lagreGrovvurdeing(behandlingId, true) }
        transactionMedBehandlingRepository {
            val behandlingMedGrovvurdering = it.hent(behandlingId)

            assertThat(behandlingMedGrovvurdering.vurderinger.grovkategorivurdering?.vurdering).isTrue()
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
    fun `når to kategoriseringvurderinger blir lagret forventer jeg å finne den siste på behandlingen`() {
        val behandlingId = transactionMedBehandlingRepository { it.opprettBehandling(JournalpostId(1)).id }
        transactionMedBehandlingRepository { it.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD) }
        Thread.sleep(100)
        transactionMedBehandlingRepository { it.lagreKategoriseringVurdering(behandlingId, Brevkode.PLIKTKORT) }
        transactionMedBehandlingRepository {
            val behandlingMedGrovvurdering = it.hent(behandlingId)

            assertThat(behandlingMedGrovvurdering.vurderinger.kategorivurdering?.vurdering).isEqualTo(Brevkode.PLIKTKORT)
        }
    }

    @Test
    fun `når struktureringsvurdering blir lagret forventer jeg å finne den på behandlingen`() {
        InitTestDatabase.dataSource.transaction {
            val repository = BehandlingRepositoryImpl(it)

            val behandlingId = repository.opprettBehandling(JournalpostId(11111)).id

            val json = """{"Test: Dokument"}"""
            repository.lagreStrukturertDokument(behandlingId, json)

            val strukturertBehandling = repository.hent(behandlingId)

            assertThat(strukturertBehandling.harBlittStrukturert()).isTrue()
            assertThat(strukturertBehandling.vurderinger.struktureringsvurdering?.vurdering).isEqualTo(json)

        }
    }

    @Test
    fun `når to struktureringsvurderinger blir lagret forventer jeg å finne den siste på behandlingen`() {
        val json = """{"Test: Dokument"}"""

        val behandlingId = transactionMedBehandlingRepository { it.opprettBehandling(JournalpostId(1)).id }
        transactionMedBehandlingRepository { it.lagreStrukturertDokument(behandlingId, """{"Test: Plakat"}""") }
        Thread.sleep(100)
        transactionMedBehandlingRepository { it.lagreStrukturertDokument(behandlingId, json) }
        transactionMedBehandlingRepository {
            val behandlingMedGrovvurdering = it.hent(behandlingId)

            assertThat(behandlingMedGrovvurdering.vurderinger.struktureringsvurdering?.vurdering).isEqualTo(json)
        }
    }

    @Test
    fun `hent behandling med id returnerer behandling og alle vurderinger`() {
        InitTestDatabase.dataSource.transaction {
            val repository = BehandlingRepositoryImpl(it)

            val behandlingId = repository.opprettBehandling(JournalpostId(11111)).id
            repository.lagreStrukturertDokument(behandlingId, """{"Test: Dokument"}""")
            repository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)
            repository.lagreGrovvurdeing(behandlingId, false)


            repository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)

            val behandling = repository.hent(behandlingId)

            assertThat(behandling.harBlittStrukturert()).isTrue()
            assertThat(behandling.harBlittKategorisert()).isTrue()
            assertThat(behandling.harBlittgrovkategorisert()).isTrue()

        }
    }

    @Test
    fun `hent behandling referanse returnerer behandling og alle vurderinger`() {
        InitTestDatabase.dataSource.transaction {
            val repository = BehandlingRepositoryImpl(it)

            val behandling = repository.opprettBehandling(JournalpostId(11111))
            val behandlingId = behandling.id
            repository.lagreStrukturertDokument(behandlingId, """{"Test: Dokument"}""")
            repository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)
            repository.lagreGrovvurdeing(behandlingId, false)


            val hentetBehandling = repository.hent(behandling.referanse)

            assertThat(hentetBehandling.harBlittStrukturert()).isTrue()
            assertThat(hentetBehandling.harBlittKategorisert()).isTrue()
            assertThat(hentetBehandling.harBlittgrovkategorisert()).isTrue()

        }
    }

    fun <T> transactionMedBehandlingRepository(fn: (behandlingRepository: BehandlingRepositoryImpl) -> T): T =
        InitTestDatabase.dataSource.transaction {
            val behandlingRepository = BehandlingRepositoryImpl(it)
            fn(behandlingRepository)
        }


}