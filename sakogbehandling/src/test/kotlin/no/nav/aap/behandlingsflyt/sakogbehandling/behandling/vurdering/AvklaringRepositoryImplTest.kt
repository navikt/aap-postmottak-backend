package no.nav.aap.behandlingsflyt.sakogbehandling.behandling.vurdering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

class AvklaringRepositoryImplTest {

    @BeforeEach
    fun clean() {
        InitTestDatabase.dataSource.transaction {
            it.execute("""TRUNCATE BEHANDLING CASCADE""")
        }
    }

    @Test
    fun `når kategoriseringvurdering blir lagret forventer jeg å finne den på behandlingen`() {
        inContext {

            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111)).id

            avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)

            val behandlingMedKategorisering = behandlingRepository.hent(behandlingId)

            assertThat(behandlingMedKategorisering.harBlittKategorisert()).isTrue()
            assertThat(behandlingMedKategorisering.vurderinger.kategorivurdering?.avklaring).isEqualTo(Brevkode.SØKNAD)

        }
    }

    @Test
    fun `når to kategoriseringvurderinger blir lagret forventer jeg å finne den siste på behandlingen`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)).id }
        inContext { avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD) }
        Thread.sleep(100)
        inContext { avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.PLIKTKORT) }
        inContext {
            val behandlingMedTemavurdering = behandlingRepository.hent(behandlingId)

            assertThat(behandlingMedTemavurdering.vurderinger.kategorivurdering?.avklaring).isEqualTo(Brevkode.PLIKTKORT)
        }
    }

    @Test
    fun `lagrer saksnummer på behandling`() {
        val saksnummer = "234234"
        val behandling = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreSakVurdeirng(behandling.id, Saksnummer(saksnummer)) }
        inContext{
            val actual = behandlingRepository.hent(behandling.id)
            assertThat(actual.vurderinger.saksvurdering?.saksnummer).isEqualTo(saksnummer)
        }

    }

    @Test
    fun `når struktureringsvurdering blir lagret forventer jeg å finne den på behandlingen`() {
        inContext {

            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111)).id

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

        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)).id }
        inContext { avklaringRepository.lagreStrukturertDokument(behandlingId, """{"Test: Plakat"}""") }
        Thread.sleep(100)
        inContext { avklaringRepository.lagreStrukturertDokument(behandlingId, json) }
        inContext {
            val behandlingMedTemavurdering = behandlingRepository.hent(behandlingId)

            assertThat(behandlingMedTemavurdering.vurderinger.struktureringsvurdering?.vurdering).isEqualTo(json)
        }
    }

    @Test
    fun `når teamavklaring blir lagret forventer jeg å finne den på behandlingen`() {
        inContext {
            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111)).id

            avklaringRepository.lagreTeamAvklaring(behandlingId, false)

            val behandlingMedTemaavklaring = behandlingRepository.hent(behandlingId)

            assertThat(behandlingMedTemaavklaring.harTemaBlittAvklart()).isTrue()
            assertThat(behandlingMedTemaavklaring.vurderinger.avklarTemaVurdering?.avklaring).isFalse()

        }
    }

    @Test
    fun `når to temaavklaringer blir lagret forventer jeg å finne den siste på behandlingen`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)).id }
        inContext { avklaringRepository.lagreTeamAvklaring(behandlingId, false) }
        Thread.sleep(100)
        inContext { avklaringRepository.lagreTeamAvklaring(behandlingId, true) }
        inContext {
            val behandlingMedTemavurdering = behandlingRepository.hent(behandlingId)

            assertThat(behandlingMedTemavurdering.vurderinger.avklarTemaVurdering?.avklaring).isTrue()
        }
    }

    @Test
    fun `behandlingsversjon blir bumpet når struktureringvurdering blir gjort`() {
        val behandling = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext{ avklaringRepository.lagreStrukturertDokument(behandling.id, """{"YOLO": true}""") }
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
    fun `behandlingsversjon blir bumpet når teamvurdering blir gjort`() {
        val behandling = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreTeamAvklaring(behandling.id, false) }
        val versjon = inContext { behandlingRepository.hent(behandling.id).versjon }

        assertThat(versjon).isEqualTo(1)
    }

    @Test
    fun `behandlingsversjon blir bumpet når saksvurdering blir utført`() {
        val behandling = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreSakVurdeirng(behandling.id, Saksnummer("wdfgsdfgbs")) }
        val versjon = inContext { behandlingRepository.hent(behandling.id).versjon }

        assertThat(versjon).isEqualTo(1)
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
            inContext {
                avklaringRepository.lagreTeamAvklaring(behandlingId, true)
            }
        }.join()

        inContext {
            val temavurdering = behandlingRepository.hent(behandlingId).vurderinger.avklarTemaVurdering?.avklaring
            assertThat(temavurdering).isNotNull().isEqualTo(true)
        }

    }


    private class Context(val behandlingRepository: BehandlingRepository, val avklaringRepository: AvklaringRepository)

    private fun <T>inContext(block: Context.() -> T): T {
        return InitTestDatabase.dataSource.transaction {
            val context = Context(BehandlingRepositoryImpl(it), AvklaringRepositoryImpl(it))
            context.let(block)
        }
    }
}