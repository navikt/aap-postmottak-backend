package no.nav.aap.postmottak.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.vurdering.AvklaringRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    fun `hent behandling med id returnerer behandling og alle vurderinger`() {
        inContext {

            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111)).id
            avklaringRepository.lagreStrukturertDokument(behandlingId, """{"Test: Dokument"}""")
            avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)
            avklaringRepository.lagreTeamAvklaring(behandlingId, false)


            avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)

            val behandling = behandlingRepository.hent(behandlingId)

            assertThat(behandling.harBlittStrukturert()).isTrue()
            assertThat(behandling.harBlittKategorisert()).isTrue()
            assertThat(behandling.harTemaBlittAvklart()).isTrue()

        }
    }

    @Test
    fun `hent behandling referanse returnerer behandling og alle vurderinger`() {
        inContext {

            val behandling = behandlingRepository.opprettBehandling(JournalpostId(11111))
            val behandlingId = behandling.id
            avklaringRepository.lagreStrukturertDokument(behandlingId, """{"Test: Dokument"}""")
            avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)
            avklaringRepository.lagreTeamAvklaring(behandlingId, false)


            val hentetBehandling = behandlingRepository.hent(behandling.referanse)

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

    private class Context(val behandlingRepository: BehandlingRepository, val avklaringRepository: AvklaringRepository)

    private fun <T>inContext(block: Context.() -> T): T {
        return InitTestDatabase.dataSource.transaction {
            val context = Context(BehandlingRepositoryImpl(it), AvklaringRepositoryImpl(it))
            context.let(block)
        }
    }
}