package no.nav.aap.postmottak.vurdering

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
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
    fun `når kategoriseringvurdering blir lagret forventer jeg å finne den på behandlingen`() {
        inContext {
            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111))

            avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)

            val behandlingMedKategorisering = dokumentbehandlingRepository.hent(behandlingId)

            assertThat(behandlingMedKategorisering.harBlittKategorisert()).isTrue()
            assertThat(behandlingMedKategorisering.vurderinger.kategorivurdering?.avklaring).isEqualTo(Brevkode.SØKNAD)
        }
    }

    @Test
    fun `når to kategoriseringvurderinger blir lagret forventer jeg å finne den siste på behandlingen`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD) }
        Thread.sleep(100)
        inContext { avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.PLIKTKORT) }
        inContext {
            val behandlingMedTemavurdering = dokumentbehandlingRepository.hent(behandlingId)

            assertThat(behandlingMedTemavurdering.vurderinger.kategorivurdering?.avklaring).isEqualTo(Brevkode.PLIKTKORT)
        }
    }


    @Test
    fun `når struktureringsvurdering blir lagret forventer jeg å finne den på behandlingen`() {
        inContext {

            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111))

            val json = """{"Test: Dokument"}"""
            avklaringRepository.lagreStrukturertDokument(behandlingId, json)

            val strukturertBehandling = dokumentbehandlingRepository.hent(behandlingId)

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
            val behandlingMedTemavurdering = dokumentbehandlingRepository.hent(behandlingId)

            assertThat(behandlingMedTemavurdering.vurderinger.struktureringsvurdering?.vurdering).isEqualTo(json)
        }
    }

    @Test
    fun `når teamavklaring blir lagret forventer jeg å finne den på behandlingen`() {
        inContext {
            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111))

            avklaringRepository.lagreTeamAvklaring(behandlingId, false)

            val behandlingMedTemaavklaring = dokumentbehandlingRepository.hent(behandlingId)

            assertThat(behandlingMedTemaavklaring.harTemaBlittAvklart()).isTrue()
            assertThat(behandlingMedTemaavklaring.vurderinger.avklarTemaVurdering?.avklaring).isFalse()

        }
    }

    @Test
    fun `når to temaavklaringer blir lagret forventer jeg å finne den siste på behandlingen`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklaringRepository.lagreTeamAvklaring(behandlingId, false) }
        Thread.sleep(100)
        inContext { avklaringRepository.lagreTeamAvklaring(behandlingId, true) }
        inContext {
            val behandlingMedTemavurdering = dokumentbehandlingRepository.hent(behandlingId)

            assertThat(behandlingMedTemavurdering.vurderinger.avklarTemaVurdering?.avklaring).isTrue()
        }
    }

    private class Context(
        val dokumentbehandlingRepository: BehandlingRepository,
        val avklaringRepository: AvklaringRepository,
        val behandlingRepository: BehandlingRepository
    )

    private fun <T>inContext(block: Context.() -> T): T {
        return InitTestDatabase.dataSource.transaction {
            val context = Context(BehandlingRepositoryImpl(it), AvklaringRepositoryImpl(it), BehandlingRepositoryImpl(it))
            context.let(block)
        }
    }
}