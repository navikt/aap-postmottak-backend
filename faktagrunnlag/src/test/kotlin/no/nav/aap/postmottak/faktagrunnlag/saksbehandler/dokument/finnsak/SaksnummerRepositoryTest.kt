package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak

import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import java.time.LocalDate

class SaksnummerRepositoryTest {

    val dataSource = InitTestDatabase.dataSource

    fun getPeriode() = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 31))
    val saksinfo: List<Saksinfo> = listOf(
        Saksinfo("sak: 1", getPeriode()),
        Saksinfo("sak: 2", getPeriode())
    )

    @AfterEach
    fun tearDown() {
        dataSource.transaction {
            it.execute(
                """
            TRUNCATE BEHANDLING CASCADE
        """.trimIndent()
            )
        }
    }

    @Test
    fun hentSaksnummre() {
        inContext {
            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(1))
            saksnummerRepository.lagreSaksnummer(behandlingId, saksinfo)

            val actual = saksnummerRepository.hentSaksnummre(behandlingId)

            assertThat(actual).isEqualTo(saksinfo)
        }
    }

    @Test
    fun `hent siste saksnummre for behandling`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }

        inContext { saksnummerRepository.lagreSaksnummer(behandlingId, saksinfo) }

        inContext { saksnummerRepository.lagreSaksnummer(behandlingId, saksinfo + Saksinfo("Sak: 3", getPeriode())) }

        inContext {
            val saker = saksnummerRepository.hentSaksnummre(behandlingId)
            assertThat(saker).size().isEqualTo(3)
        }
    }

    @Test
    fun lagreSaksnummer() {
        inContext {
            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(1))

            saksnummerRepository.lagreSaksnummer(behandlingId, saksinfo)

            val actual = connection.queryFirst(
                """
                SELECT COUNT(*) as count FROM SAKER_PAA_BEHANDLING""".trimIndent()
            ) {
                setRowMapper { it.getInt("count") }
            }

            assertThat(actual).isEqualTo(2)
        }

    }

    @Test
    fun `lagrer saksnummeravklaring på behandling`() {
        val saksnummer = "234234"
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { saksnummerRepository.lagreSakVurdering(behandlingId, Saksvurdering(saksnummer)) }
        inContext {
            val actual = saksnummerRepository.hentSakVurdering(behandlingId)
            assertThat(actual?.saksnummer).isEqualTo(saksnummer)
        }

    }

    @Test
    fun `kan ikke ha to aktive vurderinger på samme behandling`() {
        val saksnummer = "234234"
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { saksnummerRepository.lagreSakVurdering(behandlingId, Saksvurdering(saksnummer)) }

        catchThrowable {
            dataSource.transaction {
                it.execute(
                    """insert INTO SAKSVURDERING_GRUNNLAG (behandling_Id, SAKSNUMMER_AVKLARING_ID) 
             SELECT ?, id FROM SAKSNUMMER_AVKLARING LIMIT 1""".trimMargin()
                ) { setParams { setLong(1, behandlingId.toLong()) } }
            }
        }
    }

    @Test
    fun `hvis to vurderinger blir lagt på samme sak blir den første deaktivert`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { saksnummerRepository.lagreSakVurdering(behandlingId, Saksvurdering("YOLO")) }
        inContext { saksnummerRepository.lagreSakVurdering(behandlingId, Saksvurdering("SWAG")) }

        assertThat( inContext { saksnummerRepository.hentSakVurdering(behandlingId)?.saksnummer }).isEqualTo("SWAG")
    }

    private class TestContext(val connection: DBConnection) {
        val saksnummerRepository: SaksnummerRepository = SaksnummerRepository(connection)
        val behandlingRepository: BehandlingRepository = BehandlingRepositoryImpl(connection)
    }

    private fun <T> inContext(block: TestContext.() -> T): T {
        return dataSource.transaction {
            TestContext(it).let(block)
        }
    }

}