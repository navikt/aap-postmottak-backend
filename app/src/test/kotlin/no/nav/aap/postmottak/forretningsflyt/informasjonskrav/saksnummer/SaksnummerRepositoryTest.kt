package no.nav.aap.postmottak.forretningsflyt.informasjonskrav.saksnummer

import no.nav.aap.postmottak.overlevering.behandlingsflyt.Saksinfo
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import java.time.LocalDate
import javax.sql.DataSource

class SaksnummerRepositoryTest {

    val dataSource = InitTestDatabase.dataSource

    fun getPeriode() = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 31))
    val saksinfo: List<Saksinfo> = listOf(
        Saksinfo("sak: 1", getPeriode()),
        Saksinfo("sak: 2", getPeriode())
    )

    @AfterEach
    fun tearDown() {
        dataSource.transaction { it.execute("""
            TRUNCATE BEHANDLING CASCADE
        """.trimIndent()) }
    }

    @Test
    fun hentSaksnummre() {
        withContext(dataSource) {
            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(1)).id
            saksnummerRepository.lagreSaksnummer(behandlingId, saksinfo)

            val actual = saksnummerRepository.hentSaksnummre(behandlingId)

            assertThat(actual).isEqualTo(saksinfo)
        }
    }

    @Test
    fun `hent siste saksnummre for behandling`() {
        val behandlingId = withContext(dataSource) { behandlingRepository.opprettBehandling(JournalpostId(1)).id }

        withContext(dataSource) { saksnummerRepository.lagreSaksnummer(behandlingId, saksinfo) }

        withContext(dataSource) { saksnummerRepository.lagreSaksnummer(behandlingId, saksinfo + Saksinfo("Sak: 3", getPeriode())) }

        withContext(dataSource) {
            val saker = saksnummerRepository.hentSaksnummre(behandlingId)
            assertThat(saker).size().isEqualTo(3)
        }
    }

    @Test
    fun lagreSaksnummer() {
        withContext(dataSource) {
            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(1)).id

            saksnummerRepository.lagreSaksnummer(behandlingId, saksinfo)

            val actual = connection.queryFirst("""
                SELECT COUNT(*) as count FROM SAKER_PAA_BEHANDLING""".trimIndent()) {
                setRowMapper { it.getInt("count") }
            }

            assertThat(actual).isEqualTo(2)
        }

    }

    private  class TestContext(val connection: DBConnection) {
        val saksnummerRepository: SaksnummerRepository = SaksnummerRepository(connection)
        val behandlingRepository: BehandlingRepository = BehandlingRepositoryImpl(connection)
    }

    private fun <T>withContext(dataSource: DataSource, block: TestContext.() -> T): T {
        return dataSource.transaction {
            TestContext(it).let(block)
        }
    }

}