package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

class BehandlingRepositoryImplTest {


    @BeforeEach
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
            val behandling = repository.opprettBehandling(journalpostId)

            assertThat(behandling.journalpostId).isEqualTo(journalpostId)
            assertThat(behandling.typeBehandling).isEqualTo(TypeBehandling.DokumentHåndtering)
        }
    }

    @Test
    fun `når teamavklaring blir lagret forventer jeg å finne den på behandlingen`() {
        InitTestDatabase.dataSource.transaction {
            val repository = BehandlingRepositoryImpl(it)

            val behandlingId = repository.opprettBehandling(JournalpostId(11111)).id

            repository.lagreTeamAvklaring(behandlingId, false)

            val behandlingMedTemaavklaring = repository.hent(behandlingId)

            assertThat(behandlingMedTemaavklaring.harTemaBlittAvklart()).isTrue()
            assertThat(behandlingMedTemaavklaring.vurderinger.avklarTemaVurdering?.vurdering).isFalse()

        }
    }

    @Test
    fun `når to temaavklaringer blir lagret forventer jeg å finne den siste på behandlingen`() {
        val behandlingId = transactionMedBehandlingRepository { it.opprettBehandling(JournalpostId(1)).id }
        transactionMedBehandlingRepository { it.lagreTeamAvklaring(behandlingId, false) }
        Thread.sleep(100)
        transactionMedBehandlingRepository { it.lagreTeamAvklaring(behandlingId, true) }
        transactionMedBehandlingRepository {
            val behandlingMedTemavurdering = it.hent(behandlingId)

            assertThat(behandlingMedTemavurdering.vurderinger.avklarTemaVurdering?.vurdering).isTrue()
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
            val behandlingMedTemavurdering = it.hent(behandlingId)

            assertThat(behandlingMedTemavurdering.vurderinger.kategorivurdering?.vurdering).isEqualTo(Brevkode.PLIKTKORT)
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
            val behandlingMedTemavurdering = it.hent(behandlingId)

            assertThat(behandlingMedTemavurdering.vurderinger.struktureringsvurdering?.vurdering).isEqualTo(json)
        }
    }

    @Test
    fun `hent behandling med id returnerer behandling og alle vurderinger`() {
        InitTestDatabase.dataSource.transaction {
            val repository = BehandlingRepositoryImpl(it)

            val behandlingId = repository.opprettBehandling(JournalpostId(11111)).id
            repository.lagreStrukturertDokument(behandlingId, """{"Test: Dokument"}""")
            repository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)
            repository.lagreTeamAvklaring(behandlingId, false)


            repository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)

            val behandling = repository.hent(behandlingId)

            assertThat(behandling.harBlittStrukturert()).isTrue()
            assertThat(behandling.harBlittKategorisert()).isTrue()
            assertThat(behandling.harTemaBlittAvklart()).isTrue()

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
            repository.lagreTeamAvklaring(behandlingId, false)


            val hentetBehandling = repository.hent(behandling.referanse)

            assertThat(hentetBehandling.harBlittStrukturert()).isTrue()
            assertThat(hentetBehandling.harBlittKategorisert()).isTrue()
            assertThat(hentetBehandling.harTemaBlittAvklart()).isTrue()

        }
    }

    @Test
    fun `forventer at vurderingstabeller blir låst når behandlingen er låst`() {
        val behandlingId = transactionMedBehandlingRepository { it.opprettBehandling(JournalpostId(1)).id }
        thread {
            transactionMedBehandlingRepositorySuspend {
                it.hent(BehandlingId(1))
                Thread.sleep(500)
                it.lagreTeamAvklaring(behandlingId, false)
            }
        }
        thread {
            Thread.sleep(100)
            transactionMedBehandlingRepositorySuspend {
                it.lagreTeamAvklaring(behandlingId, true)
            }
        }.join()

        transactionMedBehandlingRepository {
            val temavurdering = it.hent(behandlingId).vurderinger.avklarTemaVurdering?.vurdering
            assertThat(temavurdering).isNotNull().isEqualTo(true)
        }

    }

    @Test
    fun `lagrer saksnummer på behandling`() {
        val saksnummer = "234234"
        val behandling = transactionMedBehandlingRepository { it.opprettBehandling(JournalpostId(1)) }
        transactionMedBehandlingRepository { it.lagreSakVurdeirng(behandling.id, Saksnummer(saksnummer)) }
        transactionMedBehandlingRepository {
            val actual = it.hent(behandling.id)
            assertThat(actual.vurderinger.saksvurdering?.vurdering?.saksnummer).isEqualTo(saksnummer)
        }

    }

    @Test
    fun `behandlingsversjon blir bumpet når behanlding blir endret`() {
        val behandling = transactionMedBehandlingRepository { it.opprettBehandling(JournalpostId(1)) }
        transactionMedBehandlingRepository { it.lagreSakVurdeirng(behandling.id, Saksnummer("wdfgsdfgbs")) }
        val versjon = transactionMedBehandlingRepository { it.hent(behandling.id).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    @Test
    fun `behandlingsversjon blir bumpet når teamvurdering blir gjort`() {
        val behandling = transactionMedBehandlingRepository { it.opprettBehandling(JournalpostId(1)) }
        transactionMedBehandlingRepository { it.lagreTeamAvklaring(behandling.id, false) }
        val versjon = transactionMedBehandlingRepository { it.hent(behandling.id).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    @Test
    fun `behandlingsversjon blir bumpet når kategorivurdering blir gjort`() {
        val behandling = transactionMedBehandlingRepository { it.opprettBehandling(JournalpostId(1)) }
        transactionMedBehandlingRepository { it.lagreKategoriseringVurdering(behandling.id, Brevkode.SØKNAD) }
        val versjon = transactionMedBehandlingRepository { it.hent(behandling.id).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    @Test
    fun `behandlingsversjon blir bumpet når struktureringvurdering blir gjort`() {
        val behandling = transactionMedBehandlingRepository { it.opprettBehandling(JournalpostId(1)) }
        transactionMedBehandlingRepository { it.lagreStrukturertDokument(behandling.id, """{"YOLO": true}""") }
        val versjon = transactionMedBehandlingRepository { it.hent(behandling.id).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    fun <T> transactionMedBehandlingRepository(fn: (behandlingRepository: BehandlingRepositoryImpl) -> T): T =
        InitTestDatabase.dataSource.transaction {
            val behandlingRepository = BehandlingRepositoryImpl(it)
            fn(behandlingRepository)
        }

    fun <T> transactionMedBehandlingRepositorySuspend(fn: (behandlingRepository: BehandlingRepositoryImpl) -> T): T =
        InitTestDatabase.dataSource.transaction {
            val behandlingRepository = BehandlingRepositoryImpl(it)
            fn(behandlingRepository)
        }
}