package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.DigitaliseringsvurderingRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DigitaliseringsvurderingRepositoryImplTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    @Test
    fun `når struktureringsvurdering blir lagret forventer jeg å finne den på behandlingen`() {
        inContext {

            val behandlingId =
                behandlingRepository.opprettBehandling(JournalpostId(11111), TypeBehandling.Journalføring)
            val søknadsdato = LocalDate.of(2025, 1, 15)
            val json = """{"Test: Dokument"}"""
            struktureringsvurderingRepository.lagre(
                behandlingId,
                Digitaliseringsvurdering(InnsendingType.SØKNAD, json, søknadsdato)
            )

            val struktureringsvurdering = struktureringsvurderingRepository.hentHvisEksisterer(behandlingId)

            assertThat(struktureringsvurdering?.strukturertDokument).isEqualTo(json)

        }
    }

    @Test
    fun `når to struktureringsvurderinger blir lagret forventer jeg å finne den siste på behandlingen`() {
        val json = """{"Test: Dokument"}"""
        val søknadsdato = LocalDate.of(2025, 1, 15)

        val behandlingId =
            inContext { behandlingRepository.opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring) }
        inContext {
            struktureringsvurderingRepository.lagre(
                behandlingId,
                Digitaliseringsvurdering(InnsendingType.SØKNAD, """{"Test: Plakat"}""", søknadsdato)
            )
        }
//        Thread.sleep(100)
        inContext {
            struktureringsvurderingRepository.lagre(
                behandlingId,
                Digitaliseringsvurdering(InnsendingType.SØKNAD, json, søknadsdato)
            )
        }
        inContext {
            val struktureringsvurdering = struktureringsvurderingRepository.hentHvisEksisterer(behandlingId)

            assertThat(struktureringsvurdering?.strukturertDokument).isEqualTo(json)
        }
    }

    @Test
    fun `kan ikke ha to aktive vurderinger på samme behandling`() {
        val behandlingId =
            inContext { behandlingRepository.opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring) }
        inContext {
            struktureringsvurderingRepository.lagre(
                behandlingId,
                Digitaliseringsvurdering(InnsendingType.DIALOGMELDING, null, null)
            )
        }

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
        val søknadsdato = LocalDate.of(2025, 1, 15)
        val søknadsdato2 = LocalDate.of(2025, 1, 20)
        val behandlingId =
            inContext { behandlingRepository.opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring) }
        inContext {
            struktureringsvurderingRepository.lagre(
                behandlingId,
                Digitaliseringsvurdering(InnsendingType.SØKNAD, "YOLO", søknadsdato)
            )
        }
        inContext {
            struktureringsvurderingRepository.lagre(
                behandlingId,
                Digitaliseringsvurdering(InnsendingType.SØKNAD, "SWAG", søknadsdato2)
            )
        }

        inContext {
            val actual = struktureringsvurderingRepository.hentHvisEksisterer(behandlingId)
            assertThat(actual?.strukturertDokument).isEqualTo("SWAG")
            assertThat(actual?.søknadsdato).isEqualTo(søknadsdato2)
        }
    }

    @Test
    fun `kan kopiere vurdering fra en behnadling til en annen`() {
        val journalpostId = JournalpostId(1)
        val søknadsdato = LocalDate.of(2025, 1, 15)

        val vurdering = Digitaliseringsvurdering(InnsendingType.SØKNAD, "YOLO", søknadsdato)
        inContext {
            val fraBehandling = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
            val tilBehandling = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.DokumentHåndtering)
            struktureringsvurderingRepository.lagre(fraBehandling, vurdering)
            struktureringsvurderingRepository.kopier(fraBehandling, tilBehandling)

            assertThat(struktureringsvurderingRepository.hentHvisEksisterer(tilBehandling)).isEqualTo(vurdering)
        }
    }

    private class Context(
        val struktureringsvurderingRepository: DigitaliseringsvurderingRepository,
        val behandlingRepository: BehandlingRepository
    )

    private fun <T> inContext(block: Context.() -> T): T {
        return dataSource.transaction {
            val context = Context(DigitaliseringsvurderingRepositoryImpl(it), BehandlingRepositoryImpl(it))
            context.let(block)
        }
    }
}
