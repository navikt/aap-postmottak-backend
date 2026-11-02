package no.nav.aap.postmottak.repository.joarkavstemmer

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.postmottak.joarkavstemmer.Avstemmingsrapport
import no.nav.aap.postmottak.joarkavstemmer.Avstemmingsutfall
import no.nav.aap.postmottak.joarkavstemmer.JournalpostAvstemmingsresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class AvstemmingsrapportRepositoryImplTest {
    @AutoClose
    private val dataSource = TestDataSource()

    @Test
    fun `lagrer og henter avstemmingsrapport`() {
        val rapport = Avstemmingsrapport(
            startetTidspunkt = Instant.parse("2026-11-02T03:00:00Z"),
            fullførtTidspunkt = Instant.parse("2026-11-02T03:15:00Z"),
            resultater = listOf(
                JournalpostAvstemmingsresultat(
                    journalpostId = 900000000L,
                    utfall = Avstemmingsutfall.UAVSTEMT_KELVIN,
                    kanal = "NAV_NO",
                    datoOpprettet = LocalDate.of(2026, 10, 20),
                )
            ),
        )
        dataSource.transaction { connection ->
            AvstemmingsrapportRepositoryImpl(connection).lagre(rapport)
        }

        val hentet = dataSource.transaction(readOnly = true) { connection ->
            AvstemmingsrapportRepositoryImpl(connection).hentSiste()
        }

        assertThat(hentet).isEqualTo(rapport)
    }
}
