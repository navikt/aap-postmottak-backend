package no.nav.aap.postmottak

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.postmottak.sakogbehandling.behandling.DokumentbehandlingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import kotlin.concurrent.thread

class DokumentbehandlingRepositoryTest {


    @BeforeEach
    fun clean() {
        InitTestDatabase.dataSource.transaction {
            it.execute("""TRUNCATE BEHANDLING CASCADE""")
        }
    }

    @Test
    fun `hent behandling med id returnerer behandling og alle vurderinger`() {
        inContext {

            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111))
            avklaringRepository.lagreStrukturertDokument(behandlingId, """{"Test: Dokument"}""")
            avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)
            avklaringRepository.lagreTeamAvklaring(behandlingId, false)

            avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)

            val behandling = dokumentbehandlingRepository.hentMedLås(behandlingId, null)

            assertThat(behandling.harBlittStrukturert()).isTrue()
            assertThat(behandling.harBlittKategorisert()).isTrue()
            assertThat(behandling.harTemaBlittAvklart()).isTrue()

        }
    }

    @Test
    fun `hent behandling referanse returnerer behandling og alle vurderinger`() {
        inContext {

            val behandlingsreferanse = JournalpostId(11111)
            val behandlingId = behandlingRepository.opprettBehandling(behandlingsreferanse)

            avklaringRepository.lagreStrukturertDokument(behandlingId, """{"Test: Dokument"}""")
            avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)
            avklaringRepository.lagreTeamAvklaring(behandlingId, false)


            val hentetBehandling = dokumentbehandlingRepository.hentMedLås(behandlingsreferanse, null)

            assertThat(hentetBehandling.harBlittStrukturert()).isTrue()
            assertThat(hentetBehandling.harBlittKategorisert()).isTrue()
            assertThat(hentetBehandling.harTemaBlittAvklart()).isTrue()

        }
    }

    @Test
    fun `forventer at vurderingstabeller blir låst når behandlingen er låst`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        thread {
            inContext {
                dokumentbehandlingRepository.hentMedLås(BehandlingId(1), null)
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
                dokumentbehandlingRepository.hentMedLås(behandlingId, null).vurderinger.avklarTemaVurdering?.avklaring
            assertThat(temavurdering).isNotNull().isEqualTo(true)
        }

    }

    @Test
    fun `lagrer saksnummer på behandling`() {
        val saksnummer = "234234"
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreSakVurdering(behandlingId, Saksnummer(saksnummer)) }
        inContext {
            val actual = dokumentbehandlingRepository.hentMedLås(behandlingId, null)
            assertThat(actual.vurderinger.saksvurdering?.saksnummer).isEqualTo(saksnummer)
        }

    }

    @Test
    fun `behandlingsversjon blir bumpet når behanlding blir endret`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreSakVurdering(behandlingId, Saksnummer("wdfgsdfgbs")) }
        val versjon = inContext { dokumentbehandlingRepository.hentMedLås(behandlingId, null).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    @Test
    fun `behandlingsversjon blir bumpet når teamvurdering blir gjort`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreTeamAvklaring(behandlingId, false) }
        val versjon = inContext { dokumentbehandlingRepository.hentMedLås(behandlingId, null).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    @Test
    fun `behandlingsversjon blir bumpet når kategorivurdering blir gjort`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD) }
        val versjon = inContext { dokumentbehandlingRepository.hentMedLås(behandlingId, null).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    @Test
    fun `behandlingsversjon blir bumpet når struktureringvurdering blir gjort`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreStrukturertDokument(behandlingId, """{"YOLO": true}""") }
        val versjon = inContext { dokumentbehandlingRepository.hentMedLås(behandlingId, null).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    @Test
    fun `når en behandling blir endret, vil ikke samme behandling med tidligere verjon bli endret`() {
        val behandling = inContext {
            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(1))
            dokumentbehandlingRepository.hentMedLås(behandlingId)
        }
        thread {
            inContext {
                dokumentbehandlingRepository.hentMedLås(behandling.id)
                sleep(500)
                avklaringRepository.lagreTeamAvklaring(behandling.id, false)
            }
        }
        lateinit var exception: Throwable
        val t = thread {
            inContext {
                sleep(100)
                val b = dokumentbehandlingRepository.hentMedLås(behandling.journalpostId, behandling.versjon)
                avklaringRepository.lagreKategoriseringVurdering(b.id, Brevkode.SØKNAD)
            }

        }
        t.setUncaughtExceptionHandler { _, throwable -> exception = throwable }
        t.join()
        assertThat(exception).isInstanceOf(NoSuchElementException::class.java)
    }

    private class Context(
        val dokumentbehandlingRepository: DokumentbehandlingRepository,
        val avklaringRepository: AvklaringRepository,
        val behandlingRepository: BehandlingRepository,
    )

    private fun <T> inContext(block: Context.() -> T): T {
        return InitTestDatabase.dataSource.transaction {
            val context =
                Context(DokumentbehandlingRepository(it), AvklaringRepositoryImpl(it), BehandlingRepositoryImpl(it))
            context.let(block)
        }
    }
}