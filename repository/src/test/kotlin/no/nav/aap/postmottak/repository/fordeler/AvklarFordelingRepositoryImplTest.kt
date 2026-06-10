package no.nav.aap.postmottak.repository.fordeler

import no.nav.aap.fordeler.arena.AapSystem
import no.nav.aap.fordeler.arena.AvklarFordelingVurdering
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AvklarFordelingRepositoryImplTest {

    @AutoClose
    private val dataSource = TestDataSource()

    @Test
    fun `hentVurderingHvisEksisterer returnerer null når ingen vurdering eksisterer`() {
        dataSource.transaction { connection ->
            val behandlingId = BehandlingRepositoryImpl(connection)
                .opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring)

            val result = AvklarFordelingRepositoryImpl(connection)
                .hentVurderingHvisEksisterer(behandlingId)

            assertThat(result).isNull()
        }
    }

    @Test
    fun `kan lagre og hente vurdering`() {
        val vurdering = AvklarFordelingVurdering(
            system = AapSystem.KELVIN,
            vurdertAv = "KELVIN",
            vurdertTidspunkt = LocalDateTime.now(),
        )

        val behandlingId = dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection)
                .opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring)
        }

        dataSource.transaction { connection ->
            AvklarFordelingRepositoryImpl(connection).lagreVurdering(behandlingId, vurdering)
        }

        val result = dataSource.transaction { connection ->
            AvklarFordelingRepositoryImpl(connection).hentVurderingHvisEksisterer(behandlingId)
        }

        assertThat(result?.system).isEqualTo(AapSystem.KELVIN)
        assertThat(result?.vurdertAv).isEqualTo("KELVIN")
    }

    @Test
    fun `lagring av ny vurdering deaktiverer gammel og returnerer den siste`() {
        val forsteVurdering = AvklarFordelingVurdering(
            system = AapSystem.KELVIN,
            vurdertAv = "KELVIN",
            vurdertTidspunkt = LocalDateTime.now(),
        )
        val andreVurdering = AvklarFordelingVurdering(
            system = AapSystem.ARENA,
            vurdertAv = "ARENA",
            vurdertTidspunkt = LocalDateTime.now(),
        )

        val behandlingId = dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection)
                .opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring)
        }

        dataSource.transaction { connection ->
            AvklarFordelingRepositoryImpl(connection).lagreVurdering(behandlingId, forsteVurdering)
        }

        dataSource.transaction { connection ->
            AvklarFordelingRepositoryImpl(connection).lagreVurdering(behandlingId, andreVurdering)
        }

        val result = dataSource.transaction { connection ->
            AvklarFordelingRepositoryImpl(connection).hentVurderingHvisEksisterer(behandlingId)
        }

        assertThat(result?.system).isEqualTo(AapSystem.ARENA)
        assertThat(result?.vurdertAv).isEqualTo("ARENA")
    }
}
