package no.nav.aap.postmottak.repository.under18

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.under18.Under18SamtykkeVurdering
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class Under18RepositoryImplTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun beforeEach() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    @Test
    fun `lagrer og henter søknader i innleggingsrekkefølge og hindrer dobbel kobling`() {
        val behandlingId = opprettBehandling()
        val annenBehandlingId = opprettBehandling(journalpostId = 2)
        val søknader = listOf(JournalpostId(11), JournalpostId(12))

        dataSource.transaction { connection ->
            val repository = Under18RepositoryImpl(connection)
            søknader.forEach { repository.lagreSøknad(behandlingId, it) }
        }

        val hentet = dataSource.transaction { connection ->
            Under18RepositoryImpl(connection).hentSøknader(behandlingId)
        }

        assertThat(hentet).isEqualTo(søknader)
        assertThatThrownBy {
            dataSource.transaction { connection ->
                Under18RepositoryImpl(connection).lagreSøknad(annenBehandlingId, søknader.first())
            }
        }
    }

    @Test
    fun `beholder samtykkehistorikk med én aktiv vurdering`() {
        val behandlingId = opprettBehandling()
        val historikk = listOf(
            Under18SamtykkeVurdering(
                beggeForeldreSamtykket = false,
                samtykkeJournalpostIder = listOf(JournalpostId(21))
            ),
            Under18SamtykkeVurdering(
                beggeForeldreSamtykket = true,
                samtykkeJournalpostIder = emptyList()
            )
        )
        val ventOpprettetTid = LocalDateTime.of(2026, 1, 1, 12, 0)

        dataSource.transaction { connection ->
            val repository = Under18RepositoryImpl(connection)
            repository.lagreSamtykkevurdering(behandlingId, historikk.first())
            repository.lagreVentOpprettetTid(behandlingId, ventOpprettetTid)
            repository.lagreSamtykkevurdering(behandlingId, historikk.last())
        }

        val (lagretHistorikk, aktivVurdering) = dataSource.transaction { connection ->
            val repository = Under18RepositoryImpl(connection)
            repository.hentSamtykkevurderinger(behandlingId) to
                repository.hentAktivSamtykkevurdering(behandlingId)
        }

        assertThat(lagretHistorikk).containsExactly(
            historikk.first().copy(ventOpprettetTid = ventOpprettetTid),
            historikk.last()
        )
        assertThat(aktivVurdering).isEqualTo(historikk.last())
    }

    @Test
    fun `lagrer ventetidspunkt én gang på aktiv vurdering`() {
        val behandlingId = opprettBehandling()
        val vurdering = Under18SamtykkeVurdering(
            beggeForeldreSamtykket = false,
            samtykkeJournalpostIder = emptyList()
        )
        val førsteTidspunkt = LocalDateTime.of(2026, 1, 1, 12, 0)

        val lagret = dataSource.transaction { connection ->
            val repository = Under18RepositoryImpl(connection)
            repository.lagreSamtykkevurdering(behandlingId, vurdering)
            repository.lagreVentOpprettetTid(behandlingId, førsteTidspunkt)
            repository.lagreVentOpprettetTid(behandlingId, førsteTidspunkt.plusDays(1))
            repository.hentAktivSamtykkevurdering(behandlingId)
        }

        assertThat(lagret?.ventOpprettetTid).isEqualTo(førsteTidspunkt)
    }

    private fun opprettBehandling(journalpostId: Long = 1): BehandlingId = dataSource.transaction { connection ->
        BehandlingRepositoryImpl(connection)
            .opprettBehandling(JournalpostId(journalpostId), TypeBehandling.Journalføring)
    }
}
