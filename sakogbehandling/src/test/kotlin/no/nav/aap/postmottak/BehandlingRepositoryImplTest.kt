package no.nav.aap.postmottak

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
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
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)).id }
        thread {
            inContext {
                behandlingRepository.hent(BehandlingId(1))
                Thread.sleep(500)
                avklaringRepository.lagreTeamAvklaring(behandlingId, false)
            }
        }
        thread {
            Thread.sleep(100)
            inContext { avklaringRepository.lagreTeamAvklaring(behandlingId, true) }
        }.join()

        inContext {
            val temavurdering = behandlingRepository.hent(behandlingId).vurderinger.avklarTemaVurdering?.avklaring
            assertThat(temavurdering).isNotNull().isEqualTo(true)
        }

    }

    @Test
    fun `lagrer saksnummer på behandling`() {
        val saksnummer = "234234"
        val behandling = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreSakVurdering(behandling.id, Saksnummer(saksnummer)) }
        inContext {
            val actual = behandlingRepository.hent(behandling.id)
            assertThat(actual.vurderinger.saksvurdering?.saksnummer).isEqualTo(saksnummer)
        }

    }

    @Test
    fun `behandlingsversjon blir bumpet når behanlding blir endret`() {
        val behandling = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreSakVurdering(behandling.id, Saksnummer("wdfgsdfgbs")) }
        val versjon = inContext { behandlingRepository.hent(behandling.id).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    @Test
    fun `behandlingsversjon blir bumpet når teamvurdering blir gjort`() {
        val behandling = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreTeamAvklaring(behandling.id, false) }
        val versjon = inContext { behandlingRepository.hent(behandling.id).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    @Test
    fun `behandlingsversjon blir bumpet når kategorivurdering blir gjort`() {
        val behandling = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreKategoriseringVurdering(behandling.id, Brevkode.SØKNAD) }
        val versjon = inContext { behandlingRepository.hent(behandling.id).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    @Test
    fun `behandlingsversjon blir bumpet når struktureringvurdering blir gjort`() {
        val behandling = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreStrukturertDokument(behandling.id, """{"YOLO": true}""") }
        val versjon = inContext { behandlingRepository.hent(behandling.id).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    private class Context(val behandlingRepository: BehandlingRepository, val avklaringRepository: AvklaringRepository)

    private fun <T> inContext(block: Context.() -> T): T {
        return InitTestDatabase.dataSource.transaction {
            val context = Context(BehandlingRepositoryImpl(it), AvklaringRepositoryImpl(it))
            context.let(block)
        }
    }
}