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
import org.junit.jupiter.api.assertThrows
import java.lang.Thread.sleep
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

            val behandling = behandlingRepository.hentMedLås(behandlingId, null)

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


            val hentetBehandling = behandlingRepository.hentMedLås(behandling.referanse, null)

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
                behandlingRepository.hentMedLås(BehandlingId(1), null)
                Thread.sleep(500)
                avklaringRepository.lagreTeamAvklaring(behandlingId, false)
            }
        }
        thread {
            Thread.sleep(100)
            inContext { avklaringRepository.lagreTeamAvklaring(behandlingId, true) }
        }.join()

        inContext {
            val temavurdering =
                behandlingRepository.hentMedLås(behandlingId, null).vurderinger.avklarTemaVurdering?.avklaring
            assertThat(temavurdering).isNotNull().isEqualTo(true)
        }

    }

    @Test
    fun `lagrer saksnummer på behandling`() {
        val saksnummer = "234234"
        val behandling = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreSakVurdering(behandling.id, Saksnummer(saksnummer)) }
        inContext {
            val actual = behandlingRepository.hentMedLås(behandling.id, null)
            assertThat(actual.vurderinger.saksvurdering?.saksnummer).isEqualTo(saksnummer)
        }

    }

    @Test
    fun `behandlingsversjon blir bumpet når behanlding blir endret`() {
        val behandling = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreSakVurdering(behandling.id, Saksnummer("wdfgsdfgbs")) }
        val versjon = inContext { behandlingRepository.hentMedLås(behandling.id, null).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    @Test
    fun `behandlingsversjon blir bumpet når teamvurdering blir gjort`() {
        val behandling = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreTeamAvklaring(behandling.id, false) }
        val versjon = inContext { behandlingRepository.hentMedLås(behandling.id, null).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    @Test
    fun `behandlingsversjon blir bumpet når kategorivurdering blir gjort`() {
        val behandling = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreKategoriseringVurdering(behandling.id, Brevkode.SØKNAD) }
        val versjon = inContext { behandlingRepository.hentMedLås(behandling.id, null).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    @Test
    fun `behandlingsversjon blir bumpet når struktureringvurdering blir gjort`() {
        val behandling = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreStrukturertDokument(behandling.id, """{"YOLO": true}""") }
        val versjon = inContext { behandlingRepository.hentMedLås(behandling.id, null).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    @Test
    fun `når en behandling blir endret, vil ikke samme behandling med tidligere verjon bli endret`() {
        val behandling = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        thread {
            inContext {
                behandlingRepository.hentMedLås(behandling.id)
                sleep(500)
                avklaringRepository.lagreTeamAvklaring(behandling.id, false)
            }
        }
        lateinit var exception: Throwable
        val t = thread {
            inContext {
                sleep(100)
                val b = behandlingRepository.hentMedLås(behandling.journalpostId, behandling.versjon)
                avklaringRepository.lagreKategoriseringVurdering(b.id, Brevkode.SØKNAD)
            }

        }
        t.setUncaughtExceptionHandler { _, throwable -> exception = throwable}
        t.join()
        assertThat(exception).isInstanceOf(NoSuchElementException::class.java)
    }

    private class Context(val behandlingRepository: BehandlingRepository, val avklaringRepository: AvklaringRepository)

    private fun <T> inContext(block: Context.() -> T): T {
        return InitTestDatabase.dataSource.transaction {
            val context = Context(BehandlingRepositoryImpl(it), AvklaringRepositoryImpl(it))
            context.let(block)
        }
    }
}