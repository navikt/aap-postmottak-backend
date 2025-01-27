package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.StruktureringsvurderingRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class DigitaliseringsvurderingRepositoryImplTest {

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

            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111), TypeBehandling.Journalføring)

            val json = """{"Test: Dokument"}"""
            struktureringsvurderingRepository.lagreStrukturertDokument(behandlingId,
                Digitaliseringsvurdering(InnsendingType.SØKNAD, json))

            val struktureringsvurdering = struktureringsvurderingRepository.hentStruktureringsavklaring(behandlingId)

            assertThat(struktureringsvurdering?.strukturertDokument).isEqualTo(json)

        }
    }

    @Test
    fun `når to struktureringsvurderinger blir lagret forventer jeg å finne den siste på behandlingen`() {
        val json = """{"Test: Dokument"}"""

        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring) }
        inContext { struktureringsvurderingRepository.lagreStrukturertDokument(behandlingId, Digitaliseringsvurdering(InnsendingType.SØKNAD, """{"Test: Plakat"}""")) }
        Thread.sleep(100)
        inContext { struktureringsvurderingRepository.lagreStrukturertDokument(behandlingId,
            Digitaliseringsvurdering(InnsendingType.SØKNAD, json)) }
        inContext {
            val struktureringsvurdering = struktureringsvurderingRepository.hentStruktureringsavklaring(behandlingId)

            assertThat(struktureringsvurdering?.strukturertDokument).isEqualTo(json)
        }
    }

    @Test
    fun `kan ikke ha to aktive vurderinger på samme behandling`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring) }
        inContext { struktureringsvurderingRepository.lagreStrukturertDokument(behandlingId, Digitaliseringsvurdering(InnsendingType.SØKNAD,"")) }

        catchThrowable {
            dataSource.transaction {
                it.execute(
                    """insert INTO DIGITALISERINGSVURDERING_GRUNNLAG (behandling_Id, SAKSNUMMER_AVKLARING_ID) 
             SELECT ?, id FROM SAKSNUMMER_AVKLARING LIMIT 1""".trimMargin()
                ) { setParams { setLong(1, behandlingId.toLong()) } }
            }
        }
    }

    @Test
    fun `hvis to vurderinger blir lagt på samme sak blir den første deaktivert`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring) }
        inContext { struktureringsvurderingRepository.lagreStrukturertDokument(behandlingId, Digitaliseringsvurdering(InnsendingType.SØKNAD, "YOLO")) }
        inContext { struktureringsvurderingRepository.lagreStrukturertDokument(behandlingId, Digitaliseringsvurdering(InnsendingType.SØKNAD, "SWAG")) }

        assertThat(inContext { struktureringsvurderingRepository.hentStruktureringsavklaring(behandlingId)?.strukturertDokument }).isEqualTo("SWAG")
    }

    @Test
    fun `kan kopiere vurdering fra en behnadling til en annen`() {
        val journalpostId = JournalpostId(1)
        val vurdeirng = Digitaliseringsvurdering(InnsendingType.SØKNAD, "YOLO")
        inContext {
            val fraBehandling = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
            val tilBehandling = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.DokumentHåndtering)
            struktureringsvurderingRepository.lagreStrukturertDokument(fraBehandling, vurdeirng)
            struktureringsvurderingRepository.kopier(fraBehandling, tilBehandling)

            assertThat(struktureringsvurderingRepository.hentStruktureringsavklaring(tilBehandling)).isEqualTo(vurdeirng)
        }
    }

    private class Context(
        val struktureringsvurderingRepository: StruktureringsvurderingRepository,
        val behandlingRepository: BehandlingRepository
    )

    private fun <T>inContext(block: Context.() -> T): T {
        return InitTestDatabase.dataSource.transaction {
            val context = Context(StruktureringsvurderingRepositoryImpl(it), BehandlingRepositoryImpl(it))
            context.let(block)
        }
    }
}