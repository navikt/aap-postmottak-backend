package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class StruktureringsvurderingRepositoryTest {

    val dataSource = InitTestDatabase.dataSource

    @AfterEach
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
            struktureringsvurderingRepository.lagreStrukturertDokument(behandlingId, json)

            val struktureringsvurdering = struktureringsvurderingRepository.hentStruktureringsavklaring(behandlingId)

            assertThat(struktureringsvurdering?.vurdering).isEqualTo(json)

        }
    }

    @Test
    fun `når to struktureringsvurderinger blir lagret forventer jeg å finne den siste på behandlingen`() {
        val json = """{"Test: Dokument"}"""

        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { struktureringsvurderingRepository.lagreStrukturertDokument(behandlingId, """{"Test: Plakat"}""") }
        Thread.sleep(100)
        inContext { struktureringsvurderingRepository.lagreStrukturertDokument(behandlingId, json) }
        inContext {
            val struktureringsvurdering = struktureringsvurderingRepository.hentStruktureringsavklaring(behandlingId)

            assertThat(struktureringsvurdering?.vurdering).isEqualTo(json)
        }
    }

    @Test
    fun `kan ikke ha to aktive vurderinger på samme behandling`() {
        val saksnummer = "234234"
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { struktureringsvurderingRepository.lagreStrukturertDokument(behandlingId, "") }

        catchThrowable {
            dataSource.transaction {
                it.execute(
                    """insert INTO STRUKTURERINGAVKLARING_GRUNNLAG (behandling_Id, SAKSNUMMER_AVKLARING_ID) 
             SELECT ?, id FROM SAKSNUMMER_AVKLARING LIMIT 1""".trimMargin()
                ) { setParams { setLong(1, behandlingId.toLong()) } }
            }
        }
    }

    @Test
    fun `hvis to vurderinger blir lagt på samme sak blir den første deaktivert`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { struktureringsvurderingRepository.lagreStrukturertDokument(behandlingId, "YOLO") }
        inContext { struktureringsvurderingRepository.lagreStrukturertDokument(behandlingId, "SWAG") }

        assertThat(inContext { struktureringsvurderingRepository.hentStruktureringsavklaring(behandlingId)?.vurdering }).isEqualTo("SWAG")
    }

    private class Context(
        val struktureringsvurderingRepository: StruktureringsvurderingRepository,
        val behandlingRepository: BehandlingRepository
    )

    private fun <T>inContext(block: Context.() -> T): T {
        return InitTestDatabase.dataSource.transaction {
            val context = Context(StruktureringsvurderingRepository(it), BehandlingRepositoryImpl(it))
            context.let(block)
        }
    }
}